

public class LZ4 {
	
	class DataBlock {
		int literalLenght;
		int matchLenght;
		String[] literals;
		String[] match;
		int offset;
		
		public DataBlock() {
			this.literalLenght = 0;
		    this.matchLenght = 0;
		    this.literals = new String[0];
		    this.match = new String[0];
		    int offset = 0;			
		}
		public DataBlock(int litLen, int matLen, String[] lit, String[] mat, int offSet) {
			this.literalLenght = litLen;
		    this.matchLenght = matLen;
		    this.literals = lit;
		    this.match = mat;
		    int offset = offSet;			
		}
	}
	
}