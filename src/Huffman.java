import java.util.PriorityQueue;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Huffman {

	class HuffmanNode {
		int data;
		char character;
		HuffmanNode left;
		HuffmanNode right;

		public HuffmanNode(int i, char c) {
			this.data = i;
			this.character = c;
			left = null;
			right = null;
		}

		public HuffmanNode(HuffmanNode root) {
			this.data = root.data;
			this.character = root.character;
			left = root.left;
			right = root.right;
		}
	}

	class ListComparator implements Comparator<HuffmanNode> {
		public int compare(HuffmanNode arg0, HuffmanNode arg1) {

			if (arg0.data < arg1.data)
				return -1;
			if (arg0.data > arg1.data)
				return 1;
			if (arg0.data == arg1.data) {

				if (arg0.character > arg1.character)
					return 1;
				if (arg0.character < arg1.character)
					return -1;
			}
			return 0;
		}
	}

	private static final char EMPTY_CHARACTER = 0;
	private static HashMap<Character, String> hashmap = new HashMap<Character, String>();

	public static void main(String[] args) throws NoSuchFieldException, SecurityException, IllegalArgumentException,
			IllegalAccessException, IOException {
		// Enter data using BufferReader
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String filename = null;
		System.out.println("Vai izvÄ“lÄ“ties failu 'temp.txt'? (y/n)");
		char ans = (char) System.in.read();
		if (ans == 'y' || ans == 'Y') {
			filename = "temp.txt";
			System.in.read(new byte[System.in.available()]);
		} else {
			System.in.read(new byte[System.in.available()]);
			System.out.println("Ievadiet faila nosaukumu:");
			filename = reader.readLine();
			if (filename.length() == 0) {
				filename = "temp.txt";
			}
		}
		System.out.println("Faila izmantojamÄ� faila nosaukums: " + filename);
		System.in.read(new byte[System.in.available()]);
		System.out.println("Ievadiet beigu faila nosaukumu:");
		String result_filename = reader.readLine();
		if (result_filename.length() == 0) {
			result_filename = "temp.txt";
		}
		System.out.println("Beigu faila nosaukums: " + result_filename);

		// Code page atkÄ¼Å«doÅ¡anai
		// System.setProperty("file.encoding","UTF-8");
		// Field charset = Charset.class.getDeclaredField("defaultCharset");
		// charset.setAccessible(true);
		// charset.set(null,null);
		Huffman mHuf = new Huffman();
		HuffmanNode root = null;
		List<HuffmanNode> nodeList = new ArrayList<HuffmanNode>();
		List<HuffmanNode> nodeList2 = null;

		boolean end = false;
		while (true) {

			System.out.println("IzvÄ“lÄ“ties operÄ�ciju: (0 - pabeigt darbÄ«bu; 1 - saspiest; 2 - dekodÄ“t)");

			ans = (char) System.in.read();
			System.in.read(new byte[System.in.available()]);
			System.out.println("IzvÄ“lÄ“ties metodi: (1 - Huffman; 2 - LZ4)");
			char met = (char) System.in.read();
			System.in.read(new byte[System.in.available()]);
			if (met == '1') {
				switch (ans) {
				case '1':
					// encode
					try {
						nodeList = mHuf.nodeListGen(filename, false);
						nodeList2 = new ArrayList<HuffmanNode>(nodeList);
						root = mHuf.treeGen(nodeList);
						printCode(root, "");
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						mHuf.fileEncoder(hashmap, filename, nodeList2, result_filename);
					} catch (IOException e) {
						e.printStackTrace();
					}
					end = true;
					break;
				case '2':
					// decode
					try {
						nodeList = mHuf.nodeListGen(filename, true);
						nodeList2 = new ArrayList<HuffmanNode>(nodeList);
						root = mHuf.treeGen(nodeList);
						printCode(root, "");
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						mHuf.fileDecoder(filename, root, result_filename);

					} catch (IOException e) {
						e.printStackTrace();
					}
					end = true;
					break;
				case '0':
					end = true;
					break;
				default:
					System.out.println("IevadÄ«tÄ� operÄ�cija neeksistÄ“, mÄ“Ä£iniet vÄ“lreiz");
					break;
				}
				if (end) {
					System.out.println("DarbÄ«ba beigta");
					break;
				}
			} else {
				switch (ans) {
				case '1':
					// encode
					try {
						LZ4.fileEncoder(filename, result_filename);
					} catch (IOException e) {
						e.printStackTrace();
					}
					end = true;
					break;
				case '2':
					// decode
					try {
						LZ4.fileDecoder(filename, result_filename);
					} catch (IOException e) {
						e.printStackTrace();
					}
					end = true;
					break;
				case '0':
					end = true;
					break;
				default:
					System.out.println("IevadÄ«tÄ� operÄ�cija neeksistÄ“, mÄ“Ä£iniet vÄ“lreiz");
					break;
				}
				if (end) {
					System.out.println("DarbÄ«ba beigta");
					break;
				}

			}
		}
	}

	// TODO : izÅ†emt metodi, bet pÄ“c viÅ†as principa izveidot
	// HashMap, lai vienkÄ�rÅ¡Ä�k Encode failu
	public static void printCode(HuffmanNode root, String s) {

		// base case; if the left and right are null
		// then its a leaf node and we print
		// the code s generated by traversing the tree.
		if (root.left == null && root.right == null && ((int) root.character != 0)) {

			// c is the character in the node
			System.out.println(root.character + ":" + s);
			// Write to hashmap
			hashmap.put(root.character, s);
			return;
		}

		// if we go to left then add "0" to the code.
		// if we go to the right add"1" to the code.

		// recursive calls for left and
		// right sub-tree of the generated tree.
		printCode(root.left, s + "0");
		printCode(root.right, s + "1");
	}

	// Aizpilda koka sarakstu no saspiesta faila
	public void binFill(List<HuffmanNode> tree, Reader fr) throws Exception {
		int i, iBinTotal, nodeF = 0, pos = 0;
		String sBinTotal = "";

		// PÄ�rlasÄ�m pÄ�ri vÄ�rdnÄ«cas ievaddatiem
		// NolasÄ�m kopÄ“jo baitu izmÄ“ru
		while ((i = fr.read()) != -1 && (char) i != ' ') {
			sBinTotal += (char) i;
			pos++;
		}
		iBinTotal = Integer.parseInt(sBinTotal);
		while ((i = fr.read()) != -1) { // par iet pari bitu skaitam
			pos++;
			if ((char) i == ' ')
				break;
		}

		while (pos < iBinTotal) {
			sBinTotal = "";
			i = fr.read(); // nolas simbolu
			pos++; // palielinÄ�m apstrÄ�dÄ�to baitu
			// Ja varÄ“jÄ�m nolasÄ«t simbolu, tad pieglabÄ�jam
			final char nodeC = (i != -1) ? (char) i : 0;

			if (nodeC == 0)
				throw new Exception("KÄ¼Å«da nolasot failu : Simbols netika atrasts!");

			fr.read(); // nolasÄ�m atstarpi pirms bieÅ¾uma
			while ((i = fr.read()) != -1 && (char) i != ' ') {
				sBinTotal += (char) i;
				pos++;
			}
			nodeF = Integer.parseInt(sBinTotal);

			if (tree.stream().filter(node -> node.character == nodeC).count() > 0) {
				throw new Exception("KÄ¼Å«da nolasot failu : AtkÄ�rtojas simboli vÄ�rdnÄ«cÄ�!");
			} else {
				tree.add(new HuffmanNode(nodeF, nodeC));
			}
		}
	}

	// Aizpilda koka sarakstu no plain-text faila
	public void plainFill(List<HuffmanNode> tree, Reader fr) throws IOException {
		int i;

		while ((i = fr.read()) != -1) {
			final char ch = (char) i;
			// Ja kÄ�dai virsotnei pieder tekoÅ¡ais simbols, tad mÄ“s palielinÄ�m bieÅ¾umu
			// par 1, citÄ�di
			// pievienojam to rindai.
			if (tree.stream().filter(node -> node.character == ch).count() > 0) {
				tree.parallelStream().filter(node -> node.character == ch).forEach(node -> node.data++);
			} else {
				tree.add(new HuffmanNode(1, ch));
			}
		}

	}

	public void fileEncoder(HashMap<Character, String> enc_map, String filePath, List<HuffmanNode> que, String fOut)
			throws IOException {
		// JaunÄ� faila nosaukums
		String out_filename = fOut;
		// SavÄ�c bibliotÄ“ku iekÅ¡ sevis
		String frequencies = " ";
		for (HuffmanNode node : que) {
			frequencies += node.character + " " + node.data + " ";
		}

		byte[] bytes = null;
		try {
			bytes = frequencies.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		// Atver failu lasÄ«Å¡anai
		InputStream is = null;
		try {
			is = new FileInputStream(filePath);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		Reader fr = new InputStreamReader(is, Charset.forName("UTF-8"));
		int r;
		// Faila rakstÄ«tÄ�js
		String bin_string = "";
		/*
		 * Lasa input failu pa vienai rakstzÄ«mei. Kad tÄ� sakrÄ«t ar atslÄ“gu no
		 * HashMap, tiek ierakstÄ«ts kods mainÄ«gajÄ�.
		 */
		while ((r = fr.read()) != -1) {
			// mÅ«su hashmap principÄ� ir masÄ«vs, kur indeksi ir simboli
			// lÄ«dz ar to nav nepiecieÅ¡ams veidot foreach ciklu.
			/*
			 * char c = (char) r; for (Character i : enc_map.keySet()) {//get no hashmap
			 * dabu pec asociativa array principa if (c == i) // lieks cikls un parbaude
			 * bin_string += enc_map.get(i); }
			 */

			bin_string += enc_map.get((char) r);
		}
		// IegÅ«st baitus pirms satura (vajadzÄ«bas gadÄ«jumÄ� var pieskaitÄ«t skaitli,
		// lai bÅ«tu buffer)
		int bytes_to_content = bytes.length + 2;// Å eit jÄ�dod baiti, cik skaitlim cipari nevis vnk 2, bet pec
		// labojuma ir jadod +2 jo atstarpe un 1 cipars ka liekie biti - martins
		int old_content = bytes_to_content;
		do {
			old_content = bytes_to_content;
			bytes_to_content += Integer.toString(bytes_to_content).length();
		} while (Integer.toString(old_content).length() != Integer.toString(bytes_to_content).length());
		fr.close(); // aizstaisam lasÄ«Å¡anu, iepriekÅ¡ aizmirsi pielikt.
		try {
			FileWriter fwr = new FileWriter(out_filename);
			fwr.write(bytes_to_content + " " + (8 - bin_string.length() % 8) + frequencies);// lieks space, kas arÄ«
																							// pieliek +1 baitu izmÄ“ros
			fwr.close();
			// aizveram simbolu rakstÄ«Å¡anu un atveram binÄ�ru rakstÄ«Å¡anu
			// ar karogu true priekÅ¡ append, lai rakstÄ«tu klÄ�t, nevis pÄ�rrakstÄ«tu
			FileOutputStream fos = new FileOutputStream(out_filename, true);
			// PÄ“c tam iterÄ“ pÄ�ri mainÄ«gajam, skalda daÄ¼Ä�s, katru daÄ¼u uz baitu
			// sÄ�kotneji binÄ�ro stringu uztaisam uz integer, pÄ“c tam nokÄ�stojam uz baitu
			// jo baitu naturÄ�li var izveidot tikai vÄ“rtÄ«bÄ�m lÄ«dz 127(0b01111111)

			String emptyBits = "";
			for (int i = 0; i < (8 - bin_string.length() % 8); i++) {
				emptyBits += "0";
			}
			bin_string = emptyBits + bin_string;

			for (int i = 0; i < bin_string.length(); i += 8) {
				String test_byte;
				test_byte = bin_string.substring(i, i + 8);
				// if (i+8<bin_string.length()) {
				// test_byte = bin_string.substring(i, i+8);
				// }
				// else {
				// test_byte = bin_string.substring(i, bin_string.length());
				// while (test_byte.length()<8) {
				// test_byte += "0";
				// }
				// }

				fos.write((byte) Integer.parseInt(test_byte, 2));
			}
			fos.close();

		} catch (IOException e) {
			System.out.println("Notikusi kÄ¼Å«da");
		}

		System.out.println("SaspieÅ¡ana pabeigta");
	}

	public void fileDecoder(String filePath, HuffmanNode root, String fOut) throws IOException {
		HuffmanNode curRoot = new HuffmanNode(root);
		String character = "", biti = "";
		String newFileName = fOut;
		int c;
		FileReader fr = new FileReader(filePath);
		BufferedReader br = new BufferedReader(fr);
		while ((c = br.read()) != -1) { // nolasa baitus

			if ((char) c != ' ')
				character += (char) c;
			else
				break;
		}
		while ((c = br.read()) != -1) { // nolasa bitus

			if ((char) c != ' ')
				biti += (char) c;
			else
				break;
		}
		fr.close();
		FileInputStream fin = null;
		FileWriter wr = null;
		byte byteStream[] = null;
		byte curentByteStream;
		char symbol;
		try {
			fin = new FileInputStream(filePath);
			wr = new FileWriter(newFileName);
			fin.skip((long) Integer.parseInt(character));
			int mask = -128;
			mask = (byte) ((mask & 0xff) >>> Integer.parseInt(biti));
			int j = Integer.parseInt(biti);
			while (fin.available() > 0) {
				curentByteStream = (byte) fin.read();
				// curentByteStream=byteStream[0];
				for (; j < 8; j++) {

					boolean value = (curentByteStream & (byte) mask) != 0;

					if (curRoot.left == null && curRoot.right == null && ((int) curRoot.character != 0)) {
						wr.write(curRoot.character);
						curRoot = root;
						// j--;
						// mask = (byte)(mask<<1);
					} else {
						if (value) {
							curRoot = curRoot.right;
							mask = (byte) ((mask & 0xff) >>> 1);
						} else {
							curRoot = curRoot.left;
							mask = (byte) ((mask & 0xff) >>> 1);

						}
					}
					if (curRoot.left == null && curRoot.right == null && ((int) curRoot.character != 0))
						j--;

				}
				j = 0;
				mask = -128;

			}
			wr.close();
			fin.close();
		} catch (FileNotFoundException e) {
			System.out.println("File not found" + e);
		} catch (IOException ioe) {
			System.out.println("Exception while reading file " + ioe);
		}
	}

	/**
	 * <h1>Metode, kas nolasa failu, iegÅ«st simbolu bieÅ¾umus un izveido koku.</h1>
	 * <br>
	 * 
	 * @param fPath
	 *            : SaÅ†em ceÄ¼u uz failu
	 * @param isBinary
	 *            : Nosaka vai koku veido no saspiesta vai plain-text faila
	 * @return HuffmanNode - saknes virsotne
	 * @throws IOException
	 *             Ja nevar nolasÄ«t no faila simbolu
	 */
	public List<HuffmanNode> nodeListGen(String fPath, Boolean isBinary) throws IOException {
		// Izveido prioritÄ�tes rindu, kur elementi
		// tiek kartoti pÄ“c compare metodes(simbola bieÅ¾uma)
		// Padod sÄ�kotnÄ“jos izmÄ“rus(pÄ“c noklusÄ“juma ir 11) un metodi, kas
		// salÄ«dzina elementus
		List<HuffmanNode> tree = new ArrayList<HuffmanNode>();

		// Atveram failu pirmajai lasÄ«Å¡anai
		InputStream is = new FileInputStream(fPath);
		Reader fr = new InputStreamReader(is, Charset.forName("UTF-8"));
		try {
			if (isBinary == true)
				binFill(tree, fr);
			else
				plainFill(tree, fr);
		} catch (Exception e) {
			e.getStackTrace();
		}

		// Aizveram faila lasÄ«Å¡anu
		fr.close();

		tree.sort(new ListComparator());

		return tree;
	}

	public HuffmanNode treeGen(List<HuffmanNode> tree) throws IOException {
		// PaÅ¡laik ir sakÄ�rtota rinda ar atseviÅ¡Ä·Ä�m virsotnÄ“m
		// NepiecieÅ¡ams virsotnes apvienot kokÄ�!

		// Izveidojam saknes virsotni, no kuras sÄ�ks lasÄ«t Å¡ifrÄ“tÄ�s vÄ“rtÄ«bas
		HuffmanNode root = null;

		// Izveidojam koka struktÅ«ru
		while (tree.size() > 1) {

			HuffmanNode one = tree.get(0);
			tree.remove(0);

			HuffmanNode two = tree.get(0);
			tree.remove(0);

			HuffmanNode combo = new HuffmanNode(0, EMPTY_CHARACTER);
			combo.data = one.data + two.data;
			combo.left = one;
			combo.right = two;

			root = combo;
			tree.add(combo);

			tree.sort(new ListComparator());
		}

		// atgrieÅ¾am saknes virsotni.
		return root;
	}
}
