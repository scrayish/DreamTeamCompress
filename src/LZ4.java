
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class LZ4 {

	class DataBlock {
		int literalLength;
		int matchLength;
		byte[] src;
		byte[] outData;
	}
		
	// GlobÄ�lie mainÄ«gie.
	
	static final int MFLIMIT = 12; // LZ4 blokformāta ierobežojumi
	static final int LASTLITERALS = 5;
	static final long MAX_INPUT_SIZE = 0x7E000000;
	static final int minLength = (MFLIMIT+1);
	static final int skipTrigger = 6; // lielākas vērtības palēnina nesaspiežamu datu apstrādi
	  static final int MAGIC = 0x184D2204;
	  static final int LZ4_MAX_HEADER_LENGTH =
	      4 + // magic
	      1 + // FLG
	      1 + // BD
	      8 + // Content Size
	      1; // HC
	static	long PRIME32_1 = 2654435761l;
	static	long PRIME32_2 = 2246822519l;
	static	long PRIME32_3 = 3266489917l;
	static	long PRIME32_4 =  668265263l;
	static	long PRIME32_5 =  374761393l;
	static final int MAX_INT = 255;
	static final int MIN_MATCH = 4;
	static final int MAX_DISTANCE_LOG = 16;
	static final int MAX_DISTANCE = (1 << MAX_DISTANCE_LOG) - 1;
	static final int ML_BITS = 4;
	static final int ML_MASK = (1 << ML_BITS) - 1;
	static final int LL_BITS = 8 - ML_BITS;
	static final int LL_MASK = (1 << LL_BITS) - 1;
	
	static final int INCOMPRESSIBLE = 0x80000000;
	static final int LZ4_MEMORY_USAGE = 14; // N->2^N, 14 = 16KB kešatmiņai
	static final int HASH_LOG = LZ4_MEMORY_USAGE-2;
	static final long HASH_TABLE_SIZE = 1 << LZ4_MEMORY_USAGE;
	static final int HASH_RIGHT_SHIFT_COUNT = (MIN_MATCH * 8) - (HASH_LOG+1);
	
	private static long[] posHashTable;
	
	// packBlock funkcija:
	public static int packBlock(byte[] src, int SrcLen, byte[] dest) {
		initializeCompression();
		
		int result = 0;
		// Hyper-parameters
		byte[] srcBytes = null;
		srcBytes = Arrays.copyOf(src, SrcLen);
		long ip = 0; // tekošis elements satura masīvā
		long startIndex = 0;
		long base = 0;
		long lowLimit = 0;
		long anchor = 0;
		boolean isLastLiteral = false;
		boolean isNextMatch = false;
		long iend = ip + SrcLen;
		long mflimitPlusOne = iend - MFLIMIT +1;
		long matchlimit = iend - LASTLITERALS;
		
		long op = 0; // tekošais elements saspiestajā masīvā
		long olimit = op + (MAX_DISTANCE + MAX_DISTANCE/255 + 16);
		
		long offset = 0;
		int forwardH;
		
		if (olimit<1) {return 0;}
		if (SrcLen>MAX_INPUT_SIZE) {return 0;}
		if (SrcLen<minLength) {isLastLiteral = true;/* te jāiet uz pedejo literal*/}
		if (!isLastLiteral) {
			putHashOnPos(ip, srcBytes, base);
			ip++;
			forwardH = hashPosition((int) ip, srcBytes);
			last_literal:
			for (;;) {
				
				long match;
				long token;
				long filledIp;
				
				// atrod vienādos
				long forwardIp = ip;
				int step = 1;
				int searchMatchNb = 1 << skipTrigger;
				do {
					long h = forwardH;
					long current = forwardIp-base;
					long matchIndex = posHashTable[(int) h];
					ip = forwardIp;
					forwardIp += step;
					step = searchMatchNb++ >> skipTrigger;
					if (forwardIp > mflimitPlusOne) {
						break last_literal;/*ejam uz pedejo literal*/
					}
					
					match = base + matchIndex;
					forwardH = hashPosition((int) forwardIp, srcBytes);
					posHashTable[(int) h] = current;
					if (matchIndex+MAX_DISTANCE < current) {
						continue; // esam par tālu 
					}
					long lmatch = lz4Read32((int)match, srcBytes);
					long lip = lz4Read32((int)ip, srcBytes);
					if (lmatch == lip) {
						break;
					}
				}while(true);
				
				filledIp = ip;
				while(((ip>anchor) && (match > lowLimit)) && (srcBytes[(int) match-1] == srcBytes[(int) ip-1])) {
					ip--;
					match--;
				}
				
				long litlength = ip-anchor;
				token = op++;
				if (op + litlength + (2 + 1 + LASTLITERALS) + (litlength/255)>olimit) {
					return 0; // cannot compress within budget
				}
				
				if (litlength >= LL_MASK) {
					int len = (int) (litlength - LL_MASK);
					dest[(int) token] = (byte) (LL_MASK<<ML_BITS);
					for (; len >= 255; len -=255) {
						dest[(int) op++] = (byte) 255;
					}
					dest[(int) op++] = (byte) len; // pieskaitam atlikumu
				}else {
					dest[(int)token] = (byte) (litlength<<ML_BITS);
				}
				System.arraycopy(srcBytes,(int) anchor, dest,(int) op, (int) (litlength));
				op+=litlength;
				
				next_match:
				for(;;){
					dest[(int)op] = (byte)(ip-match);
					op++;
					dest[(int)op] = (byte)((ip-match)<<8); // ierakstam offset
					op++;
					
					long matchCode;
					matchCode = LZ4_count(ip+MIN_MATCH, match+MIN_MATCH, matchlimit, srcBytes);
					ip += matchCode + MIN_MATCH;		
					
					if ((op + (1+LASTLITERALS) + (matchCode+240)/255)> olimit) {
						return 0;
					}
					
					if (matchCode >= ML_MASK) {
						dest[(int) token] += ML_MASK;
						matchCode -= ML_MASK;
						dest[(int) op] = (byte) 0xFF;
						dest[(int) op+1] = (byte) 0xFF;
						dest[(int) op+2] = (byte) 0xFF;
						dest[(int) op+3] = (byte) 0xFF;
						while (matchCode >= 4*255) {
							op +=4;
							dest[(int) op] = (byte) 0xFF;
							dest[(int) op+1] = (byte) 0xFF;
							dest[(int) op+2] = (byte) 0xFF;
							dest[(int) op+3] = (byte) 0xFF;
							matchCode -= 4*255;
						}
						op += matchCode/255;
						dest[(int)op++] = (byte)(matchCode % 255);
					}else {
						dest[(int)token] += (byte)(matchCode);
					}
					anchor = ip;
					if (ip >= mflimitPlusOne) break;
					putHashOnPos(ip-2, srcBytes, base);
					int h = hashPosition((int) ip, srcBytes);
					int current = (int) ((int) ip-base);
					long matchIndex = posHashTable[(int) h];
					match = base + matchIndex;
					posHashTable[(int) h] = current;
					long lmatch = lz4Read32((int)match, srcBytes);
					long lip = lz4Read32((int)ip, srcBytes);
					if ((lmatch == lip) && (matchIndex + MAX_DISTANCE >= current)) {
						token = op++;
						dest[(int)token] = 0;
						continue next_match;
					}
					break;
				}
				forwardH = hashPosition((int) ++ip, srcBytes);
				
				
			}
		}
		// last literals
		int lastRun = (int)(iend - anchor);
		if(op + lastRun + 1 + ((lastRun+255-LL_MASK)/255)>olimit) {
			return 0;
		}
		
		if (lastRun >= LL_MASK) {
			int accumulator = lastRun - LL_MASK;
			dest[(int) op++] = (byte) (LL_MASK<<ML_BITS) ;
			for(;accumulator >= 255; accumulator -=255) {
				dest[(int) op++] = (byte) 255;
			}
			dest[(int) op++] = (byte) accumulator;
		}else {
			dest[(int) op++] = (byte) (lastRun<<ML_BITS);
		}
		System.arraycopy(srcBytes,(int) anchor, dest,(int) op, (int) (lastRun));
		ip = anchor + lastRun;
		op += lastRun;
		
		result = (int) op;
		
		return result;
	}
		
	public static long LZ4_count(long pIn, long pMatch, long pLimit, byte[] src) {
		long pStart = pIn;
		if (pIn < pLimit-(4-1)) {
			long diff =  (lz4Read32((int)pMatch, src) ^ lz4Read32((int)pIn, src));
			if (diff <1) {
				pIn+=4; pMatch +=4;
			}else {
				long r = 0;
				for (int i = 0; i<32; i++) {
					if ((((byte) diff) & 1) == 1) {
						r = i;
						break;
					}else {
						diff = diff >>>1;
					}
				}
	            return (r>>3);
			}
		}
		
		while (pIn < pLimit-(4-1)) {
			long diff =  (lz4Read32((int)pMatch, src) ^ lz4Read32((int)pIn, src));
			if (diff<1) {pIn+=4; pMatch +=4; continue;}
			long r = 0;
			for (int i = 0; i<32; i++) {
				if ((((byte) diff) & 1) == 1) {
					r = i;
					break;
				}else {
					diff = diff >>>1;
				}
			}
            pIn += (r>>3);
            return (pIn - pStart);
		}
		
		if( (pIn<(pLimit-1)) && (lz4Read16((int)pMatch, src) == lz4Read16((int)pIn, src))){
			pIn +=2;
			pMatch +=2;
		}
		if ((pIn<pLimit && src[(int)pMatch] == src[(int)pIn])) {pIn++;}
		return (pIn - pStart);
	}
	
	public static long lz4Read32(int p, byte[] src) {
		ByteBuffer temp = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		System.arraycopy(src, p, temp.array(), 0, 4);
		long seq =  (temp.getInt());
		seq = seq & 0x00000000FFFFFFFFl;
		return seq;
	}
	
	public static int lz4Read16(int p, byte[] src) {
		ByteBuffer temp = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
		System.arraycopy(src, p, temp.array(), 0, 2);
		int seq = (int) (temp.getShort());
		seq = seq & 0x0000FFFF;
		return seq;
	}
	
	public static int hashPosition(int p, byte[] src) {
		long seq = lz4Read32(p, src);
		seq *= 2654435761L;
		seq = seq & 0x00000000ffffffffl;
		seq = (seq ) >> (HASH_RIGHT_SHIFT_COUNT);
		
		int h = (int) seq;
		return h;
	}
	
	public static void putHashOnPos(long p, byte[] src, long base) {
		int h = hashPosition((int)p, src);
		posHashTable[h] = (p-base);
	}
	
	// InicializÄ“ posHashTable
	public static void initializeCompression() {
		if (posHashTable == null) {
			posHashTable = new long[(int) HASH_TABLE_SIZE];
		}
		
		Arrays.fill(posHashTable, 0);
	}
		
	
	// unpackBlock funkcija:
	public int unpackBlock(byte[] src, int srcSize, byte[] dest) {
			int srcPos = 0;
			int destPos = 0;

			int runCode = 0;
			int literalLength = 0;
			int matchLength = 0;

			int distance = 0;
			int copyPos = 0;

			while (srcPos < srcSize) {

				runCode = src[srcPos++];

				// parkope literalus
				literalLength = (runCode >> ML_BITS);
				if (literalLength == LL_MASK) {
					while (src[srcPos] == MAX_INT) {
						literalLength += MAX_INT;
						srcPos++;
					}

					literalLength += src[srcPos++];
				}

				System.arraycopy(src, srcPos, dest, destPos, literalLength);
				destPos += literalLength;

				// parbaude uz faila beigam
				if (srcPos >= srcSize) {
					break;
				}

				// iegut attelumu
				distance = (src[srcPos] << 8) | src[srcPos + 1];
				srcPos += 1;
				srcPos += 2;

				copyPos = destPos - distance;

				// iegust match length
				matchLength = runCode & ML_MASK;
				if (matchLength == ML_MASK) {
					while (src[srcPos] == MAX_INT) {
						matchLength += MAX_INT;
						srcPos++;
					}

					matchLength += src[srcPos++];
				}
				matchLength += MIN_MATCH;

				// pārkopē atkārtojošos virkni
				while (matchLength-- > 0) {
					dest[destPos++] = dest[copyPos++];
				}
			}
			return destPos;
	}
	
	public static long xxhrot132(long x, int r){
		long a = x<<r;
		long b = x>>(32-r);
		long c = a | b;
		return c;
	}
	
	public static long xx32Round(long rng, long input) {
		long seed = rng;
		seed += input * PRIME32_2;
		seed = seed & 0x00000000FFFFFFFFl;
		seed = xxhrot132(seed, 13);
		seed *= PRIME32_1;
		seed = seed & 0x00000000FFFFFFFFl;
		return seed;
	}
	
	public static long xx32_avalanche(long hash) {
		long h32 = hash;
		h32 ^= h32 >> 15;
		h32 *= PRIME32_2;
		h32 = h32 & 0x00000000FFFFFFFFl;
		h32 ^= h32 >> 13;
		h32 *= PRIME32_3;
		h32 = h32 & 0x00000000FFFFFFFFl;
		h32 ^= h32 >> 16;
		return h32;
	}
	
	public static long PROCONE(long hash, int ip, byte[] buf) {
		long h32 = hash;
		h32 += buf[ip] * PRIME32_5;
		h32 = h32 & 0x00000000FFFFFFFFl;
		//ip++;
		h32 = xxhrot132(h32, 11) * PRIME32_1;
		h32 = h32 & 0x00000000FFFFFFFFl;
		return h32;
	}
	
	public static long PROCFOUR(long hash, int ip, byte[] buf) {
		long h32 = hash;
		h32 += lz4Read32(ip, buf) * PRIME32_3;
		h32 = h32 & 0x00000000FFFFFFFFl;
		//ip +=4;
		h32 = xxhrot132(h32, 17) * PRIME32_4;
		h32 = h32 & 0x00000000FFFFFFFFl;
		return h32;
	}
	
	public static long xxhFinalize(long hash, int ip, int len, byte[] buf) {
		
		int p = ip;
		long h32 = hash;
		switch(len&15) {
			case 12:	h32 = PROCFOUR(h32, p, buf);p +=4;
			case 8: 	h32 = PROCFOUR(h32, p, buf);p +=4;
			case 4:		h32 = PROCFOUR(h32, p, buf);p +=4;
						return xx32_avalanche(h32);
			
			case 13:	h32 = PROCFOUR(h32, p, buf);p +=4;
			case 9:		h32 = PROCFOUR(h32, p, buf);p +=4;
			case 5:		h32 = PROCFOUR(h32, p, buf);p +=4;
						h32 = PROCONE(h32, p, buf);	p++;	
						return xx32_avalanche(h32);
			
			case 14:	h32 = PROCFOUR(h32, p, buf);p +=4;
			case 10:	h32 = PROCFOUR(h32, p, buf);p +=4;
			case 6:		h32 = PROCFOUR(h32, p, buf);p +=4;
						h32 = PROCONE(h32, p, buf);p++;
						h32 = PROCONE(h32, p, buf);	p++;
						return xx32_avalanche(h32);
						
			case 15:	h32 = PROCFOUR(h32, p, buf);p +=4;
			case 11:	h32 = PROCFOUR(h32, p, buf);p +=4;
			case 7:		h32 = PROCFOUR(h32, p, buf);p +=4;
			case 3:		h32 = PROCONE(h32, p, buf);p++;
			case 2:		h32 = PROCONE(h32, p, buf);p++;
			case 1:		h32 = PROCONE(h32, p, buf);p++;
			case 0:		return xx32_avalanche(h32);
		
		}
		return h32; //nevajadzētu būt iespējamam šeit nokļūt
		
	}
	
	public static long xx32Hash(byte[] header, int offset, int len, int seed) {

		int ip = offset;
		int iend = ip+len;
		long hash;
		if (len>=16) {
			int limit = iend-15;
			long v1 = seed + PRIME32_1 + PRIME32_2;
			long v2 = seed + PRIME32_2;
			long v3 = seed + 0;
			long v4 = seed - PRIME32_1;
			
			do {
				v1 = xx32Round(v1, lz4Read32(ip,header));
				ip+=4;
				v2 = xx32Round(v2, lz4Read32(ip,header));
				ip+=4;
				v3 = xx32Round(v3, lz4Read32(ip,header));
				ip+=4;
				v4 = xx32Round(v4, lz4Read32(ip,header));
				ip+=4;
			}while (ip<limit);
			
			hash = xxhrot132(v1, 1) + xxhrot132(v2, 7)
				+ xxhrot132(v3, 12) + xxhrot132(v4, 18);
		}else {
			hash = seed + PRIME32_5;
		}
		
		hash += len;		
		
		return xxhFinalize(hash, ip, len&15, header);
	}
	
	// faila iekodÄ“Å¡anas funkcija:
	public static void fileEncoder(String InFilePath, String OutFileName) throws IOException {
		// Atver failu lasÄ«Å¡anai:
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(InFilePath);
			int delimPos = (InFilePath.lastIndexOf("\\") > -1) ? InFilePath.lastIndexOf("\\") : InFilePath.lastIndexOf("//");
			String path = InFilePath.substring(0,delimPos+1);
			os = new FileOutputStream(path+OutFileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		
		byte FLAG = 0b01100000; // versija =1, block independant, block checksum = false, content size = false, content checksum = false, reserved, dictID 
		byte BLOCK = 0b01000000; // reserved, 100 = 4 = 64KB blocksize, reserved
		ByteBuffer header = ByteBuffer.allocate(LZ4_MAX_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
		header.putInt(MAGIC);
		header.put(FLAG);
		header.put(BLOCK);
		long hash = (xx32Hash(header.array(), 4, header.position() - 4, 0) >> 8) & 0xFF; // izveido hash galvenei
		header.put((byte)hash);
		os.write(header.array(), 0, header.position());
		
		
		ByteBuffer content = ByteBuffer.allocate(MAX_DISTANCE).order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer BlockLength = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		byte[] toWrite = null;
		byte[] content_out = new byte[MAX_DISTANCE + (MAX_DISTANCE/255) + 16];
		int frameMask;
		int readByte;
		while ((readByte = is.read()) != -1) {
		      content.put((byte) readByte);
		}
		int compressedLength = packBlock(content.array(), content.position(),content_out );
		if (compressedLength >= content.position()) {
		      compressedLength = content.position();
		      toWrite = Arrays.copyOf(content.array(), compressedLength);
		      frameMask = INCOMPRESSIBLE;
		    } else {
		    	toWrite = content_out;
		    	frameMask = 0;
		}
		
		BlockLength.putInt(0, compressedLength | frameMask); // ierakstam cik daudz baitus aiznem ievaditais saturs | ierakstam pazimi ka ir/nav saspiests
	    os.write(BlockLength.array());
	    os.write(toWrite, 0, compressedLength); // ierakstam saspiesto saturu
	    BlockLength.putInt(0, 0); // freims beidzas ar 4 tukshiem baitiem
	    os.write(BlockLength.array()); // ierakstam EndMark(tukshos baitus)
	    
		is.close();
		os.close();
		
	}

	


	// faila dekodÄ“Å¡anas funkcija:
	public static void fileDecoder(String InFilePath, String OutFileName) throws IOException {
		// Atver failu lasÄ«Å¡anai:				
				// Lasa info no faila:
				String content = null;
				byte[] cont = readLineByLineJava8(InFilePath);
			    
				// byte[], kur tiks izvadÄ«ts saturs;
				byte[] content_out = new byte[999999999];
				
				LZ4 comp = new LZ4();
				comp.unpackBlock(cont, cont.length, content_out);
				FileWriter wr=null;
				wr= new FileWriter(OutFileName);
				wr.write(new String(content_out, StandardCharsets.UTF_8));
				wr.close();
	}
	
	
    private static byte[] readLineByLineJava8(String filePath) throws IOException 
    {
    	File file = new File(filePath);
    	  //init array with file length
    	  byte[] bytesArray = new byte[(int) file.length()]; 

    	  FileInputStream fis = new FileInputStream(file);
    	  fis.read(bytesArray); //read file into bytes[]
    	  fis.close();
    				
    	  return bytesArray;
    }
    
    private static String readAllBytesJava7(String filePath) 
    {
        String content = "";
        try
        {
            content = new String ( Files.readAllBytes( Paths.get(filePath) ) );
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        return content;
    }
}

