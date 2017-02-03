package havabol;

import java.io.*;
import java.util.*;

public class Scanner {
	public String sourceFileNm = "";
	public ArrayList<String> sourceLineM = new ArrayList<String>();
	public SymbolTable symbolTable = new SymbolTable(); // symbolTable
	public char[] textCharM = new char[512]; // char array for src line
	public int iSourceLineNr = 0; // source line number
	public int iColPos = 0; // column position
	public Token currentToken = new Token(""); // next token
	public Token nextToken = new Token(""); // current token
	private BufferedReader reader; // bufferedreader for our file
	private String buf = ""; // String to read source lines into
	private static final String delimiters = " \t;:() \'\"=!<>+-*/[]#,^\n"; // terminate a token
	private static final String operators = "+-*/<>!=#^"; // possible operators
	private static final String separators = "():;[],"; // possible separators 
	private boolean bPrintFlag = false; // print flag for our queue.
	private ArrayList<String> printListM = new ArrayList<String>(); // print queue for source lines
	
	/***
	 * Construct method for Scanner class
	 * <p>
	 * Takes in a file name and symbol table and assigns them to the
	 * appropriate attributes. It then opens the file and reads in
	 * each line and adds it to sourceLineM, which is our ArrayList
	 * of Strings. It then calls getNext() once to load the next token.
	 * 
	 * @param sourceFileNm		String containing the name of the input file
	 * @param symbolTable		SymbolTable to be used by the interpreter
	 * @throws IOException		Exception if reader cannot get next line
	 */
	
	public Scanner(String sourceFileNm, SymbolTable symbolTable) throws IOException
	{
		this.sourceFileNm = sourceFileNm;
		this.symbolTable = symbolTable;
		// creates a BufferedReader to read in the file
		// Placed in a try/catch to error check file
		try 
		{
			reader = new BufferedReader(new FileReader(sourceFileNm));
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		// while loop to read in all lines of source file
		while((buf = reader.readLine()) != null)
		{
			sourceLineM.add(buf);
		}
		getNext();
	}
	
	/***
	 * getNext() loads the next Token into its currentToken. It then
	 * grabs the next token from the source line and loads it into nextToken.
	 * <p>
	 * Functionally, loads the previous nextToken into currentToken. Then
	 * determines the next token depending on the characters in the source
	 * code line. When the token is obtained, it calls setClassif to
	 * determine what the token actually is. Finally, the tokenStr for
	 * nextToken is returned.
	 * 
	 * @return classStr		classStr is the tokenStr of the nextToken.
	 */
	
	public String getNext()
	{
		int iBeginPos = 0; // index pointer
		int iEndPos = 0; // index pointer
		String tStr = ""; // String buffer
		String classStr = ""; // will be our return String
		// checks if scanner has reached the end of a source line
		// then reset the column position and increments line number
		if(iColPos == sourceLineM.get(iSourceLineNr).length())
		{
			iColPos = 0;
			iSourceLineNr++;
		}
		// checks if the flag to print the source lines is set,
		// if so, iterates through print queue to print.
		if(bPrintFlag)
		{
			for(String line : printListM)
			{
				System.out.print(line);
			}
			int listLen = printListM.size();
			for(int idx = listLen - 1; idx >= 0 ; idx--)
			{
				printListM.remove(idx);
			}
			bPrintFlag = false;
		}
		// When a new source line is encountered, it is added to
		// the array list that will serve as the print queue.
		if(iColPos == 0)
		{
			try
			{
				if(!sourceLineM.get(iSourceLineNr).isEmpty())
				{
					printListM.add(String.format("%d %s\n", iSourceLineNr + 1, sourceLineM.get(iSourceLineNr)));
				}
				// if the current source line is empty, it will iterate
				// through the source file until the next non empty
				// source line is found.
				else
				{
					while(sourceLineM.get(iSourceLineNr).isEmpty())
					{
						printListM.add(String.format("%d %s\n", iSourceLineNr + 1, sourceLineM.get(iSourceLineNr)));
						iSourceLineNr++;
					}
					printListM.add(String.format("%d %s\n", iSourceLineNr + 1, sourceLineM.get(iSourceLineNr)));
				}
				bPrintFlag = true;
			}
			// When a source line number that is out of bounds of the
			// source list, it sets the token to EOF and returns a blank string.
			catch(IndexOutOfBoundsException ie)
			{
				currentToken.primClassif = Token.EOF;
				currentToken.tokenStr = "";
				nextToken.printToken();
				return "";
			}
		}
		// Assign next token to current token
		// load the next source line and convert to char array
		currentToken = nextToken;
		tStr = sourceLineM.get(iSourceLineNr);
		textCharM = tStr.toCharArray();
		// automatically skips white space in line
		while(textCharM[iColPos] == ' ')
		{
			iColPos++;
		}
		iBeginPos = iColPos;
		// iterates through source line while no delimiters are encountered.
		while(delimiters.indexOf(textCharM[iColPos]) == -1)
		{
			iColPos++;
		}
		// if block handles when a token of single char is encountered.
		if(iBeginPos == iColPos)
		{
			// checks if token is a string, and continues until string is
			// terminated or if there is an error
			if(textCharM[iBeginPos] == '\'' || textCharM[iBeginPos] == '"')
			{
				iColPos++;
				while(textCharM[iColPos] != textCharM[iBeginPos] || textCharM[iColPos-1] == '\\')
				{
					iColPos++;
					if(iColPos >= textCharM.length) {
						System.err.println("Mismatched single/double quote in string!!!");
						System.err.println("Line: " + iColPos + " Column: " + iSourceLineNr);
						throw new IllegalArgumentException(sourceLineM.get(iSourceLineNr));
					}
				}
				// Sets the end of the string token and sets the attributes 
				// of the token appropriately
				iEndPos = iColPos++;
				classStr = new String(textCharM, iBeginPos +  1, (iEndPos - 1) - iBeginPos);
				nextToken = new Token(classStr);
				nextToken.iColPos = iBeginPos;
				nextToken.iSourceLineNr = iSourceLineNr;
				nextToken.primClassif = Token.OPERAND;
				nextToken.subClassif = Token.STRING;
			}
			// if it did not encounter a string, it encountered a
			// separator. This sets the token accordingly.
			else
			{
				classStr  = new String(textCharM, iBeginPos, 1);
				nextToken = new Token(classStr);
				nextToken.iColPos = iBeginPos;
				nextToken.iSourceLineNr = iSourceLineNr;
				setClassif(classStr);
				iColPos++;
			}
		}
		// else block if a normal token is encountered. Sets the
		// attributes accordingly.
		else
		{
			iEndPos = iColPos;
			classStr = new String(textCharM, iBeginPos, iEndPos - iBeginPos);
			nextToken = new Token(classStr);
			nextToken.iColPos = iBeginPos;
			nextToken.iSourceLineNr = iSourceLineNr;
			setClassif(classStr);
		}
		return classStr;
	}
	
	/***
	 * setClassif takes the token from getNext() and determines
	 * the primClassif and subClassif.
	 * <p>
	 * It determines the class and subclass. While doing so will check
	 * for invalid syntax in current tokens. If encountered, it will throw
	 * an exception for the the error.
	 * 
	 * @param classStr		token string passed from getNext()
	 */
	
	private void setClassif(String classStr)
	{
		int intFlag = 0;  // flag if token is an integer
		int floatFlag = 0; // flag is token is a float
		char[] tokenCharsM = classStr.toCharArray(); // char array of source line
		// if token is of length 1, it will determine what kind of token
		// it is. It can be a separator, an operator, an integer, or identifier
		if(tokenCharsM.length == 1)
		{
			if(separators.indexOf(tokenCharsM[0]) != -1)
			{
				nextToken.primClassif = Token.SEPARATOR;
			}
			else if(operators.indexOf(tokenCharsM[0]) != -1)
			{
				nextToken.primClassif = Token.OPERATOR;
			}
			else if(Character.isDigit(tokenCharsM[0]))
			{
				nextToken.primClassif = Token.OPERAND;
				nextToken.subClassif = Token.INTEGER;
			}
			else
			{
				nextToken.primClassif = Token.OPERAND;
				nextToken.subClassif = Token.IDENTIFIER;
			}
		}
		// If it is longer than length it will determine what kind of
		// operand it is. If integer or float is malformed, an exception
		// is raised.
		else if(Character.isDigit(tokenCharsM[0]))
		{
			intFlag = 1;
			for(int i = 1; i < tokenCharsM.length; i++){
				if(tokenCharsM[i] == '.')
				{
					if(floatFlag == 1)
					{
						System.err.println("Float constant has more than 1 decimal!!!");
						System.err.println("Line: " + iColPos + " Column: " + iSourceLineNr);
						throw new NumberFormatException(sourceLineM.get(iSourceLineNr));
						
					}
					else
					{
						floatFlag = 1;
					}
				}
				else if(!(Character.isDigit(tokenCharsM[i])))
				{
					System.err.println("Invalid character in numeric constant!!!");
					System.err.println("Line: " + iColPos + " Column: " + iSourceLineNr);
					throw new NumberFormatException(sourceLineM.get(iSourceLineNr));
				}
			}
		}
		// Assigns attributes according to flags that were set above.
		if(floatFlag == 1)
		{
			nextToken.primClassif = Token.OPERAND;
			nextToken.subClassif = Token.FLOAT;
		}
		else if(intFlag == 1)
		{
			nextToken.primClassif = Token.OPERAND;
			nextToken.subClassif = Token.INTEGER;
		}
		else if(tokenCharsM.length > 1)
		{
			nextToken.primClassif = Token.OPERAND;
			nextToken.subClassif = Token.IDENTIFIER;
		}
	}
}
