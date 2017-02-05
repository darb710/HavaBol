package havabol;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Scanner {
	
	private String sourceFileNm;
	private String[] sourceLineM;
	private char[] textCharM;
	public int iSourceLineNr;
	public int iColPos;
	public Token currentToken;
	public Token nextToken;
	public SymbolTable symbolTable;
	private final static String delimiters = " \t;:()\'\"=!<>+-*/[]#,^\n"; //terminate a token
	
	/**
	 * This is the Scanner constructor. It will read in from the given file
	 * storing all lines in sourceLineM, and will automatically turn the first line into
	 * a char array stored in textCharM
	 * <p>
	 * The 2 tokens used for storage are also instantiated, as well as iColPos and
	 * iSourceLineNr, both to 0
	 * 
	 * @param sourceFileNm	File to be read in from and stored
	 * @param symbolTable	Symbol table which will be implemented in the future
	 */
	public Scanner(String sourceFileNm, SymbolTable symbolTable) 
	{
		this.sourceFileNm = sourceFileNm;
		
		//Read the file in line by line with a BufferedReader
		try 
		{
			BufferedReader in = new BufferedReader(new FileReader(this.sourceFileNm));
			String str;	
			//Variable line count demands an arrayList for storage
			ArrayList<String> list = new ArrayList<String>();
			//This will move through the entire file until EOF
			while((str = in.readLine()) != null)
			{
				list.add(str);
			}
			//Convert the array list to a simple array
			sourceLineM = list.toArray(new String[0]);
			//Instantiate the first source line to textCharM
			textCharM = sourceLineM[0].toCharArray();
			//Make sure that the first line isn't blank, to prevent bugs in getNext
			checkForBlankLine();
			in.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		//Instantiate the reast of the variables
		this.symbolTable = symbolTable;
		currentToken = new Token("");
		nextToken = new Token("");
		iColPos = 0;
		iSourceLineNr = 0;
	}
	
	/**
	 * getNext will handle the input text from the source file, assigning
	 * a token its column position, its source line, as well as the text 
	 * itself. Effectively returns a String containing the token text
	 * <p>
	 * getNext will also set the first character of the next token. It will 
	 * automatically call a subroutine to set the classifications of the 
	 * token, and will iterate to new lines as appropriate. This is also
	 * one of the ugliest pieces of code I've ever written.
	 * 
	 * @return String containing the text of the current Token. An empty string if EOF
	 */
	public String getNext() 
	{
		//Set the current token to the next token, as well as make a new token for nextToken
		currentToken = nextToken;
		nextToken = new Token("");
		//Trim off any whitespace as that isn't relevant for out token
		currentToken.tokenStr = currentToken.tokenStr.trim();
		
		//This checks for EOF, if true will return an empty token
		if(currentToken.primClassif == 6)
		{
			return currentToken.tokenStr;
		}
		//This checks if we're at the beginning of a line. If we are, print the line
		//we're working with as well as the line number
		if(iColPos == 0)
		{
			System.out.println((iSourceLineNr + 1) + " " + sourceLineM[iSourceLineNr]);
		}
		//Update the line numbers to appropriate values, as well as the iColPos
		currentToken.iSourceLineNr = iSourceLineNr;
		nextToken.iSourceLineNr = iSourceLineNr;
		currentToken.iColPos = iColPos;

		int i = iColPos;
		//This checks if we're working with a string. The current token could be a quote
		//in the case of nextToken being a quote not surrounded by whitespace
		if(currentToken.tokenStr.indexOf('"') != -1
				|| currentToken.tokenStr.indexOf('\'') != -1)
		{
			//This seems peculiar, and part of the reason that my code is so ugly.
			//In the case that nextToken was a quote, iColPos got iterated forward, like
			//for any nextToken. This puts iColPos 1 after the quote, which isn't true if
			//currentToken naturally started the string. The subroutine captureString
			//needs the iColPos to be on the quote however, not after
			iColPos--;
		}
		//Check to see if the nextToken, now currentToken was a delimiter, if it was, 
		//we can just classify it instead of capturing more onto the token
		else if(!(currentToken.tokenStr.length() > 0 && delimiters.indexOf(currentToken.tokenStr.charAt(0)) != -1))
		{
			//This checks all of the characters in textCharM, seeing if they are a delimiter,
			//if they aren't adding them to the token, and otherwise breaking
			for (i = iColPos; i < textCharM.length; i++)
			{		
				if(delimiters.indexOf(textCharM[i]) != -1)
				{
					//Normally we would want to break on a delimiter, however if we have
					//nothing captured, that means that the current delimiter is in fact
					//the token. We capture it, iterate to the next character so as to not
					//get stuck on the delimiter
					if(currentToken.tokenStr.length() == 0)
					{
						currentToken.tokenStr += textCharM[i];
						i++;
					}
					break;
				}
				currentToken.tokenStr += textCharM[i];
			}
			
		}
		//This will set the primary and secondary classifications for the token. If it 
		//comes back false the subroutines will have already printed their error messages,
		//and all we need to do is set currentToken to an empty string to terminate
		if(setPrimClassif() == false)
		{
			currentToken.tokenStr = "";
			return currentToken.tokenStr;
		}
		//If we haven't reached the end of the line, and the current token isn't a string,
		//we will set nextToken to the next character, and move iColPos up accordingly.
		//The reason why it matters that currentToken isn't a string, is that captureString
		//will be moving iColPos on its own and moving iColPos here would make things 
		//inaccurate
		if(i < textCharM.length && currentToken.subClassif != 5)
		{
			nextToken.iColPos = i;
			nextToken.tokenStr += textCharM[i];
			iColPos = i + 1;
		}
		//If we're here then we've reached the end of the line, and we need to move to the next
		else
		{
			iSourceLineNr++;
			//Check for end of file
			if(iSourceLineNr < sourceLineM.length)
			{
				textCharM = sourceLineM[iSourceLineNr].toCharArray();
				//This will make sure the next line isn't a blank line
				if(checkForBlankLine())
				{
					iColPos = 0;
					nextToken = new Token("");
					return currentToken.tokenStr;
				}
			}
			//If we reach this point, we have reached EOF and just need to set it
			else
			{
				nextToken.primClassif = 6;
			}
		}

		return currentToken.tokenStr;
	}
	
	/**
	 * setPrimClassif sets the primary classification for the currentToken,
	 * and automatically call setSubClassif, setting its subclass as
	 * well
	 * <p>
	 * This method will not technically set an operands primary class, it relies
	 * on setSubClassif for that
	 * 
	 * @return boolean indicating a successful classification, otherwise false
	 */
	public boolean setPrimClassif()
	{
		//This enormous block will check for any operator
		if(currentToken.tokenStr.indexOf('+') != -1)
		{
			currentToken.primClassif = 2;
			return true;
		} 
		else if(currentToken.tokenStr.indexOf('-') != -1)
		{
			currentToken.primClassif = 2;
			return true;
		} 
		else if(currentToken.tokenStr.indexOf('*') != -1)
		{
			currentToken.primClassif = 2;
			return true;
		}
		else if(currentToken.tokenStr.indexOf('/') != -1)
		{
			currentToken.primClassif = 2;
			return true;
		}
		else if(currentToken.tokenStr.indexOf('<') != -1)
		{
			currentToken.primClassif = 2;
			return true;
		}
		else if(currentToken.tokenStr.indexOf('>') != -1)
		{
			currentToken.primClassif = 2;
			return true;
		}
		else if(currentToken.tokenStr.indexOf('!') != -1)
		{
			currentToken.primClassif = 2;
			return true;
		}
		else if(currentToken.tokenStr.indexOf('=') != -1)
		{
			currentToken.primClassif = 2;
			return true;
		}
		else if(currentToken.tokenStr.indexOf('#') != -1)
		{
			currentToken.primClassif = 2;
			return true;
		}
		else if(currentToken.tokenStr.indexOf('^') != -1)
		{
			currentToken.primClassif = 2;
			return true;
		}
		//Start checking for separators
		else if(currentToken.tokenStr.indexOf('(') != -1)
		{
			currentToken.primClassif = 3;
			return true;
		}
		else if(currentToken.tokenStr.indexOf(')') != -1)
		{
			currentToken.primClassif = 3;
			return true;
		}
		else if(currentToken.tokenStr.indexOf(':') != -1)
		{
			currentToken.primClassif = 3;
			return true;
		}
		else if(currentToken.tokenStr.indexOf(';') != -1)
		{
			currentToken.primClassif = 3;
			return true;
		}
		else if(currentToken.tokenStr.indexOf('[') != -1)
		{
			currentToken.primClassif = 3;
			return true;
		}
		else if(currentToken.tokenStr.indexOf(']') != -1)
		{
			currentToken.primClassif = 3;
			return true;
		}
		else if(currentToken.tokenStr.indexOf(',') != -1)
		{
			currentToken.primClassif = 3;
			return true;
		}
		//If the token is neither an operator or a separator, it is an operand, and if it is
		//an operand, it could potentially have a subclass that needs to be set.
		else if(setSubClassif())
		{
			return true;
		}
		//If it was none of the above, this was an invalid input
		return false;
	}
	
	/**
	 * setSubClassif will check and set the subclass on an operand
	 * <p>
	 * This will also set the primary subclass of the current token to an
	 * operand if successful, as only operands can have subclasses at the moment
	 * 
	 * @return boolean value, true indicating success and false otherwise
	 */
	public boolean setSubClassif()
	{
		//Check for string literal.
		if(currentToken.tokenStr.indexOf('"') != -1 || currentToken.tokenStr.indexOf('\'') != -1)
		{
			//This will set the currentToken to be more than just a quote, capturing the
			//entire string
			iColPos = captureString(iColPos);
			nextToken.iColPos = iColPos;
			//If iColPos was set to -1, then captureString failed
			if(iColPos == -1)
			{
				System.out.println("Improper quotation marks for String on line " + (currentToken.iSourceLineNr + 1));
				return false;
			}
			currentToken.primClassif = 1;
			currentToken.subClassif = 5;
			//Chop off the quotes
			currentToken.tokenStr = currentToken.tokenStr.substring(1, currentToken.tokenStr.length() - 1);
		}
		//Check for a numeric, does not assumes Integer until second check later on
		else if(currentToken.tokenStr.indexOf('0') != -1
				|| currentToken.tokenStr.indexOf('1') != -1
				|| currentToken.tokenStr.indexOf('2') != -1
				|| currentToken.tokenStr.indexOf('3') != -1
				|| currentToken.tokenStr.indexOf('4') != -1
				|| currentToken.tokenStr.indexOf('5') != -1
				|| currentToken.tokenStr.indexOf('6') != -1
				|| currentToken.tokenStr.indexOf('7') != -1
				|| currentToken.tokenStr.indexOf('8') != -1
				|| currentToken.tokenStr.indexOf('9') != -1)
		{
			//This will cycle through the currentToken, checking for any non decimal non numerical
			//value, erroring if it finds something
			for(int i = 0; i < currentToken.tokenStr.length(); i++)
			{
				if(Character.getNumericValue(currentToken.tokenStr.indexOf(i)) != -1
						&& currentToken.tokenStr.indexOf(i) != '.')
				{
					System.out.println("Invalid numeric token, unexpected non-decimal non-numeral, Line: " 
							+ (iSourceLineNr + 1) + " ColPos: " + iColPos + " Token: " + currentToken.tokenStr);
					return false;
				}
			}
			currentToken.primClassif = 1;
			currentToken.subClassif = 2;
		}
		//If it is not a numeric or string, it must be an identifier
		else
		{
			//Cycle through the identifier checking for any delimiter, numeric, or decimal,
			//in order to through an error
			for(int i = 0; i < currentToken.tokenStr.length(); i++)
			{
				if(Character.getNumericValue(currentToken.tokenStr.indexOf(i)) != -1
						&& currentToken.tokenStr.indexOf(i) != '.'
						&& delimiters.indexOf(currentToken.tokenStr.indexOf(i)) != -1)
				{
					System.out.println("Invalid identifier, cannot contain a numeric, decimal, or"
							+ " delimiter. Token: " + currentToken.tokenStr + " ColPos: " + iColPos
							+ " Line: " + iSourceLineNr);
					return false;
				}
			}
			currentToken.primClassif = 1;
			currentToken.subClassif = 1;
		}
		//If a numeric was found earlier, it was assumed to be an integer, here we will check
		//for a float
		if(currentToken.subClassif == 2)
		{
			//Verify there is a decimal at all
			if(currentToken.tokenStr.indexOf('.') != -1)
			{
				//If there was a decimal before, and then the last instance of a decimal
				//and the first instance of a decimal are different, there are multiple
				//decimals, making this an invalid float
				if(currentToken.tokenStr.indexOf('.') !=  currentToken.tokenStr.lastIndexOf('.'))
				{
					System.out.println("Too many decimal points, invalid float, Token: " 
							+ currentToken.tokenStr + " ColPos: " + iColPos + " Line: " + iSourceLineNr);
					return false;
				}
				currentToken.subClassif = 3;
			}
		}
		return true;
	}
	
	/**
	 * checkForBlank line will check to make sure the next line when looking at the source
	 * code isn't blank, and if it is, skipping right over it.
	 * <p>
	 * 
	 * @return boolean true on success and false on EOF
	 */
	public boolean checkForBlankLine()
	{
		//Cycle through the source line array, and for any blank line, skipping it 
		while(sourceLineM[iSourceLineNr].trim().length() == 0)
		{
			//Check to make sure we're not at the end of the array
			if(iSourceLineNr < sourceLineM.length)
			{
				iSourceLineNr++;
				textCharM = sourceLineM[iSourceLineNr].toCharArray();
			}
			//If we are at the end of the array, break
			else
			{
				break;
			}
		}
		//If we found a line that wasn't blank, return true
		if(textCharM.length != 0)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * captureString will take a currentToken containing a quote, and complete the string,
	 * returning the value without escape characters and with quotes
	 * <p>
	 * captureString must have the iColPos be where the quote was on the textCharM, otherwise
	 * it will return an empty string with 2 quotes
	 * 
	 * @param i	This is the current iColPos
	 * @return integer representing where the last quotation was found + 1, so as the be on
	 * 	the next character
	 */
	public int captureString(int i)	
	{
		int j;
		//This will store the quotation mark being used. That is to say, we don't want to 
		//terminate when the intial quote was a " on a '
		char quote = currentToken.tokenStr.charAt(0);
		
		//This will iterate through the textCharM, capturing any non terminating value
		//and adding it to currentToken
		for(j = i + 1; j < textCharM.length; j++)
		{
			//If we stumble upon a backslash, we need to worry about an escape character
			if(textCharM[j] == '\\')
			{
				//If following the backslash is a quote, escape that quote, that is to say
				//capture that quote, leave behind the backslash, and move the cursor to
				//after the escape character
				if(textCharM[j+1] == '\'' || textCharM[j+1] == '"')
				{
					currentToken.tokenStr += textCharM[j+1];
					j += 1;
					//We need to continue here as we've already made a capture, and we need
					//to make more checks, for instance if there are multiple escape characters
					continue;
				}
			}
			currentToken.tokenStr += textCharM[j];
			//If the current character is a quote, we've captured our full string, return
			//j iterated by one so that way we won't try to capture another string upon
			//the next call to getNext
			if(textCharM[j] == quote)
			{
				return j + 1;
			}
		}
		return -1;
	}
}
