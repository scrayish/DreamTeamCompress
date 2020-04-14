
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
	
	// Globālie mainīgie.
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
		int seq = 0; // Pretty much slīdošais logs.
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
				extraLiteral = literalLength - (LZ4Codec.RUN_MASK - 1);
				dest[runCodePos++] = extraLiteral;
				destPos++;
				destPos = encodeLength(dest, destPos, literalLength - LL_MASK);
			} else {
				dest[runCodePos] = (byte) (literalLength << ML_BITS);
			}
			
			// Kopē Literals
			System.arraycopy(srcBytes, anchor, dest, destPos, literalLength);
			destPos += literalLength;
			
			// Little endian offset:
			if (distance < 0) {
				throw new RuntimeException();
			}
			dest[destPos++] = (byte) (distance & 0xFF);
			dest[destPos++] = (byte) ((distance >> 8) & 0xFF);
			
			// KKāds meme
			srcPos += MIN_MATCH;
			refPos += MIN_MATCH;
			anchor = srcPos;
			while (srcPos < srcSize && srcBytes[srcPos] == srcBytes[refPos]) {
				srcPos++;
				refPos++;
			}
			
			matchLength = srcPos - anchor;
			
			// Iekodē match length
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
	
	// Inicializē posHashTable
	public void initializeCompression() {
		if (posHashTable == null) {
			posHashTable = new int[HASH_TABLE_SIZE];
		}
		
		Arrays.fill(posHashTable, -MAX_DISTANCE);
	}
	
	private static final int encodeLength(byte[] dest, int destPos, int length) {
        if (length  < 0) {
            throw new IllegalArgumentException();
        }

        while (length > 254) {
            length -= 255;
            dest[destPos++] = (byte) 255;
        }

        return destPos;
    }
	
	
	// unpackBlock funkcija:
	public void unpackBlock() {
		
	}
	
	
	// faila iekodēšanas funkcija:
	public void fileEncoder(String InFilePath, String OutFileName) {
		// Atver failu lasīšanai:
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
		
		// byte[], kur tiks izvadīts saturs;
		byte[] content_out = null;
		
	}
	
	
	// faila dekodēšanas funkcija:
	public void fileDecoder() {
		
	}
	
	public static void main(String[] args) {
		
	}
	
}

