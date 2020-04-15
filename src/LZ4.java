
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
	
	static final int MAX_INT = 255;
	static final int MIN_MATCH = 4;
	static final int MAX_DISTANCE_LOG = 16;
	static final int MAX_DISTANCE = (1 << MAX_DISTANCE_LOG) - 1;
	static final int ML_BITS = 4;
	static final int ML_MASK = (1 << ML_BITS) - 1;
	static final int LL_BITS = 8 - ML_BITS;
	static final int LL_MASK = (1 << LL_BITS) - 1;
	
	static final int INCOMPRESSIBLE = 128;
	static final int LZ4_MEMORY_USAGE = 14; // N->2^N, 14 = 16KB kešatmiņai
	static final int HASH_LOG = LZ4_MEMORY_USAGE-2;
	static final long HASH_TABLE_SIZE = 1 << LZ4_MEMORY_USAGE;
	static final int HASH_RIGHT_SHIFT_COUNT = (MIN_MATCH * 8) - (HASH_LOG+1);
	
	private static long[] posHashTable;
	
	// packBlock funkcija:
	public static int packBlock(byte[] src, byte[] dest) {
		initializeCompression();
		
		int result = 0;
		// Hyper-parameters
		byte[] srcBytes = null;
		srcBytes = Arrays.copyOf(src, src.length);
		long ip = 0; // tekošis elements satura masīvā
		long startIndex = 0;
		long base = 0;
		long lowLimit = 0;
		long anchor = 0;
		boolean isLastLiteral = false;
		boolean isNextMatch = false;
		long iend = ip + src.length;
		long mflimitPlusOne = iend - MFLIMIT +1;
		long matchlimit = iend - LASTLITERALS;
		
		long op = 0; // tekošais elements saspiestajā masīvā
		long olimit = op + (MAX_DISTANCE + MAX_DISTANCE/255 + 16);
		
		long offset = 0;
		int forwardH;
		
		if (olimit<1) {return 0;}
		if (src.length>MAX_INPUT_SIZE) {return 0;}
		if (src.length<minLength) {isLastLiteral = true;/* te jāiet uz pedejo literal*/}
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
				while(((ip>anchor) && (match > lowLimit)) && (srcBytes[(int) match-1] == srcBytes[(int) ip])) {
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
				{
					dest[(int)op++] = (byte)(ip-match);
					dest[(int)op++] = (byte)((ip-match)<<8); // ierakstam offset
					
					long matchCode;
					matchCode = LZ4_count(ip+MIN_MATCH, match+MIN_MATCH, matchlimit, srcBytes);
					ip += matchCode + MIN_MATCH;		
					
					if ((op + (1+LASTLITERALS) + (matchCode+240)/255)> olimit) {
						return 0;
					}
					
					if (matchCode >= ML_MASK) {
						dest[(int) token] += ML_MASK;
						matchCode -= ML_MASK;
						dest[(int) op++] = (byte) 0xFF;
						dest[(int) op++] = (byte) 0xFF;
						dest[(int) op++] = (byte) 0xFF;
						dest[(int) op++] = (byte) 0xFF;
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
						break next_match;
					}
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
	
	public static void lastLiteral() {
		
	}
	
	
	public static long LZ4_count(long pIn, long pMatch, long pLimit, byte[] src) {
		long pStart = pIn;
		if (pIn < pLimit-(32-1)) {
			long diff =  (lz4Read32((int)pMatch, src) ^ lz4Read32((int)pIn, src));
			if (diff <1) {
				pIn+=32; pMatch +=32;
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
		
		while (pIn < pLimit-(32-1)) {
			long diff =  (lz4Read32((int)pMatch, src) ^ lz4Read32((int)pIn, src));
			if (diff<1) {pIn+=32; pMatch +=32; continue;}
			long r = 0;
			for (int i = 0; i<32; i++) {
				if ((((byte) diff) & 1) == 1) {
					r = i;
					break;
				}else {
					diff = diff >>>1;
				}
			}
            pIn += r;
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
	
	
	// faila iekodÄ“Å¡anas funkcija:
	public void fileEncoder(String InFilePath, String OutFileName) throws IOException {
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
		byte[] content_out = new byte[MAX_DISTANCE + (MAX_DISTANCE/255) + 16];
		int readByte;
		while ((readByte = is.read()) != -1) {
		      os.write(readByte);
		    }
		os.flush();
		
		
		
	}
	
	
	// faila dekodÄ“Å¡anas funkcija:
	public void fileDecoder() {
		
	}
	
	public static void main(String[] args) throws IOException {
			
		byte[] data2 = "Drumstalām miltus sajauc ar cukuriem, kanēli, sāli, tad pievieno mīkstu sviestu un visu labi samīci, ieliec ledusskapī uz vismaz 30 minūtēm, tad veido drumstalas, plucinot mīklu pa maziem gabaliņiem un ber uz izceptās ābolkūkas. Pēc tam kūku cep vēl 15–20 minūtes.".getBytes("UTF-8");
		
		
		
		// byte[], kur tiks izvadÄ«ts saturs;
		byte[] content_out = new byte[MAX_DISTANCE + (MAX_DISTANCE/255) + 16];
		ByteBuffer test = ByteBuffer.allocate(data2.length).order(ByteOrder.LITTLE_ENDIAN);
		test.put(data2);
		int compressedLength = packBlock(test.array(), content_out);
		FileOutputStream fos = new FileOutputStream(new File("test.lz4"));
		fos.write(content_out, 0, compressedLength);
		fos.close();
		
	}
	
}
