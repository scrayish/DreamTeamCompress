
import java.io.*;
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
	static final int MAX_INT = 255;
	static final int MIN_MATCH = 4;
	static final int MAX_DISTANCE_LOG = 16;
	static final int MAX_DISTANCE = 1 << MAX_DISTANCE_LOG;
	static final int ML_BITS = 4;
	static final int ML_MASK = (1 << ML_BITS) - 1;
	static final int LL_BITS = 8 - ML_BITS;
	static final int LL_MASK = (1 << LL_BITS) - 1;
	
	static final int INCOMPRESSIBLE = 128;
	static final int HASH_LOG = 17;
	static final int HASH_TABLE_SIZE = 1 << HASH_LOG;
	static final int HASH_RIGHT_SHIFT_COUNT = MIN_MATCH * 8 - HASH_LOG;
	
	private int[] posHashTable;
	
	// packBlock funkcija:
	public int packBlock(String src, byte[] dest) {
		initializeCompression();
		
		// Hyper-parameters
		byte[] srcBytes = null;
		try {
			srcBytes = src.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int srcPos = 0;
		int srcSize = srcBytes.length;
		int srcLimit = srcSize - MIN_MATCH;
		int destPos = 0;
		int seq = 0; // Pretty much slÄ«doÅ¡ais logs.
		int hashVal = 0;
		int refPos = 0;
		int distance = 0;
		int step = 1;
		int limit = INCOMPRESSIBLE;
		int anchor = 0;
		int runCodePos = 0;
		int literalLength = 0;
		int matchLength = 0;
		
		while (srcPos < srcLimit) {
			seq = (srcBytes[srcPos] << 24) | (srcBytes[srcPos + 1] << 16)
					| (srcBytes[srcPos + 2] << 8) | srcBytes[srcPos + 3];
			hashVal = ((int) (seq * 2654435761L) >>> HASH_RIGHT_SHIFT_COUNT);
			
			refPos = posHashTable[hashVal];
			posHashTable[hashVal] = srcPos;
			
			distance = srcPos - refPos;
			
			
			if (distance >= MAX_DISTANCE
					|| seq != ((srcBytes[srcPos] << 24) | (srcBytes[srcPos + 1] << 16)
					| (srcBytes[srcPos + 2] << 8) | srcBytes[srcPos + 3])) {
				if (srcPos - anchor > limit) {
					limit <<= 1;
					step += 1 + (step >> 2);
				}
				
				srcPos += step;
				continue;
			}
			
			
			if (step > 1) {
				posHashTable[hashVal] = refPos;
				srcPos -= (step - 1);
				step = 1;
				continue;
			}
			
			
			limit = INCOMPRESSIBLE;
			literalLength = srcPos - anchor;
			runCodePos = destPos;
			destPos++;
			
			if (literalLength > (LL_MASK - 1)) {
				dest[runCodePos] = (byte) (LL_MASK << ML_BITS);
				destPos = encodeLength(dest, destPos, literalLength - LL_MASK);
			} else {
				dest[runCodePos] = (byte) (literalLength << ML_BITS);
			}
			
			// KopÄ“ Literals
			System.arraycopy(srcBytes, anchor, dest, destPos, literalLength);
			destPos += literalLength;
			
			// Little endian offset:
			if (distance < 0) {
				throw new RuntimeException();
			}
			dest[destPos++] = (byte) (distance & 0xFF);
			dest[destPos++] = (byte) ((distance >> 8) & 0xFF);
			
			// KKÄ�ds meme
			srcPos += MIN_MATCH;
			refPos += MIN_MATCH;
			anchor = srcPos;
			while (srcPos < srcSize && srcBytes[srcPos] == srcBytes[refPos]) {
				srcPos++;
				refPos++;
			}
			
			matchLength = srcPos - anchor;
			
			// IekodÄ“ match length
			if (matchLength > (ML_MASK - 1)) {
				dest[runCodePos] |= (byte) ML_MASK;
				destPos = encodeLength(dest, destPos, matchLength - ML_MASK);
			} else {
				dest[runCodePos] |= (byte) matchLength;
			}
			
			anchor = srcPos;
		}
		
		literalLength = srcSize - anchor;
		if (literalLength > (LL_MASK - 1)) {
			dest[destPos++] = (byte) (LL_MASK << ML_BITS);
			destPos = encodeLength(dest, destPos, literalLength - LL_MASK);
		} else {
			dest[destPos++] = (byte) (literalLength << ML_BITS);
		}
		
		System.arraycopy(srcBytes, anchor, dest, destPos, literalLength);
		destPos += literalLength;
		
		return destPos;
	}
	
	// InicializÄ“ posHashTable
	public void initializeCompression() {
		if (posHashTable == null) {
			posHashTable = new int[HASH_TABLE_SIZE];
		}
		
		Arrays.fill(posHashTable, - MAX_DISTANCE);
	}
	
	private static final int encodeLength(byte[] dest, int destPos, int length) {
        if (length  < 0) {
            throw new IllegalArgumentException();
        }

        while (length > MAX_INT - 1 {
            length -= MAX_INT;
            dest[destPos++] = (byte) MAX_INT;
        }

        return destPos;
    }
	
	
	// unpackBlock funkcija:
	public int unpackBlock() (byte[] src, int srcSize, byte[] dest) {
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
				literalLength = (runCode >> LZ4Codec.ML_BITS);
				if (literalLength == LZ4Codec.LL_MASK) {
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
				matchLength = runCode & LZ4Codec.ML_MASK;
				if (matchLength == LZ4Codec.ML_MASK) {
					while (src[srcPos] == MAX_INT) {
						matchLength += MAX_INT;
						srcPos++;
					}

					matchLength += src[srcPos++];
				}
				matchLength += LZ4Codec.MIN_MATCH;

				// pārkopē atkārtojošos virkni
				while (matchLength-- > 0) {
					dest[destPos++] = dest[copyPos++];
				}
			}
			return destPos;
	}
	
	
	// faila iekodÄ“Å¡anas funkcija:
	public void fileEncoder(String InFilePath, String OutFileName) {
		// Atver failu lasÄ«Å¡anai:
		InputStream is = null;
		try {
			is = new FileInputStream(InFilePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Reader r = new InputStreamReader(is, Charset.forName("UTF-8"));
		
		// Lasa info no faila:
		Path path = Paths.get(InFilePath);
		String content = null;
		try {
			content = Files.readString(path, StandardCharsets.UTF_8);


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// byte[], kur tiks izvadÄ«ts saturs;
		byte[] content_out = null;
		
	}
	
	
	// faila dekodÄ“Å¡anas funkcija:
	public void fileDecoder() {
		
	}
	
	public static void main(String[] args) {
		
	}
	
}

