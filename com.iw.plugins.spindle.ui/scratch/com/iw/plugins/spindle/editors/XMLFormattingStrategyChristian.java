package test.editor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;

/**
 * default formatting strategy. It uses the partitioning of the underlying document
 * to determine the different elements for formatting
 * 
 * @author cse
 * @version $Revision$
 */
public class XMLFormattingStrategyChristian implements XMLContentFormatter.FormattingStrategy 
{
	/**
	 * Helper class which has the ability to walk over the partitions in the underlying 
	 * document in both directions (forward and backwards).
	 */
	private static class PartitionWalker
	{
		private boolean modeForward;
		private IDocument fDocument;
		private int fLastOffset, fNextOffset, fEndOffset;
		
		/**
		 * create an instance to walk forward for a given length starting at the given offset
		 */
		PartitionWalker(IDocument document, int offset, int length)
		{
			modeForward = true;
			fDocument = document;
			fNextOffset = fLastOffset = offset;
			fEndOffset = offset+length;
		}
		
		/**
		 * create an instance to walk only backward until the document start, starting at the given offset
		 */
		PartitionWalker(IDocument document, int offset)
		{
			modeForward = false;
			fDocument = document;
			fNextOffset = fLastOffset = offset;
		}

		public ITypedRegion next() throws BadLocationException
		{
			if(!modeForward)
				throw new UnsupportedOperationException("calling next on backward walker not allowed");

			if(fNextOffset >= fEndOffset)
				return null;
				
			ITypedRegion region = fDocument.getPartition(fNextOffset);
			fLastOffset = region.getOffset();
			int rLen = region.getLength();
			
			if(fLastOffset < fNextOffset) { //first partition overlaps start offset
				region = new TypedRegion(
					fNextOffset, rLen - fNextOffset + fLastOffset, region.getType());
			}
			else if(fLastOffset + rLen > fEndOffset) { //last partition overlaps end 
				region = new TypedRegion(
					fLastOffset, fEndOffset - fLastOffset, region.getType());
			}
			fNextOffset = fLastOffset+rLen;
			return region;
		}

		public ITypedRegion previous() throws BadLocationException
		{
			if(modeForward)
				throw new UnsupportedOperationException("calling previous on forward walker not allowed");
				
			if(fLastOffset == 0)
				return null;
				
			ITypedRegion region = fDocument.getPartition(fLastOffset-1);
			fLastOffset = region.getOffset();
			fNextOffset = region.getOffset()+region.getLength();
			return region;
		}
	}

	/**
	 * line info struct administered by LineWalker
	 */	
	private static class LineInfo
	{
		int offset, dataOffset, delimiterLength, posIndex1=-1, posIndex2, delta;
		String data;
		int[] positions;

		LineInfo(int offset, String data, int delimiterLength) {
			this.offset = offset;
			this.dataOffset = offset;
			this.delimiterLength = delimiterLength;
			this.data = data;
		}
		/**
		 * remove leading whitespace (modifying the internal state)
		 * @return the data string after removing whitespace
		 */
		public String trimData() {
			int i=0;
			for(; i < data.length() && data.charAt(i) <= ' '; i++) ;
			if(i > 0) {
				dataOffset += i;
				data = data.substring(i, data.length());
			}
			return data;
		}
		/**
		 * @param positions the positions array
		 */
		public void recordPositionIndex(int[] positions) 
		{
			this.positions = positions;
			int i=0;
			for(; i < positions.length && positions[i] < offset; i++) ;
			if(i < positions.length) {
				posIndex1 = i;
				int endOffset = dataOffset+data.length()+delimiterLength;
				for(; i < positions.length && positions[i] < endOffset; i++) ;
				posIndex2 = i;
			}
		}
		/**
		 * record the write offset for later processing in {@link #updatePositions()}
		 * @param offset the offset at which this line was written into the formatted output
		 */
		public void setWriteOffset(int offset)
		{
			delta = offset - dataOffset;
		}
		/**
		 * update the positions within the range of this object such that they are correct 
		 * relative to the previously set write offset.
		 */
		public void updatePositions() 
		{
			if(posIndex1 >= 0) {
				for(int i=posIndex1; i<posIndex2; i++) {
					positions[i] += delta;
				}
			}
		}
	}

	/**
	 * @deprecated use LineWalker
	 */
	private static class LineWalkerX
	{
		private List lines = new ArrayList();
		private Iterator iterator;
		
		public LineWalkerX(IDocument document, IRegion region, String delimiter, int[] positions) 
			throws BadLocationException
		{
			int delimiterLength = delimiter.length();
			int textOffset = region.getOffset();
			String text = document.get(textOffset, region.getLength());
			int i1=0, count=0;
			for(int i=0; i<text.length();) {
				int j=0;
				for(;j<delimiterLength && text.charAt(i) == delimiter.charAt(j); i++, j++) ;
				if(j == delimiterLength) {
					if(count > 0 || i1 < i-j) {
						LineInfo info = new LineInfo(textOffset+i1, text.substring(i1, i-j), delimiter.length());
						info.recordPositionIndex(positions);
						lines.add(info);
					}
					i1 = i;
					count++;
				} else {
					i++;
				}
			}
			if(i1 < text.length()) {
				LineInfo info = new LineInfo(textOffset+i1, text.substring(i1, text.length()), 0);
				info.trimData();
				//only the add last segment if it contains some text
				if(info.data.length() > 0) {
					info.recordPositionIndex(positions);
					lines.add(info);
				}
			}
			iterator = lines.iterator();
		}

		/**
		 * @return whether there are any more lines
		 */
		public boolean hasMoreLines() 
		{
			return iterator.hasNext();
		}

		/**
		 * @return the next LineInfo
		 * @throws java.util.NoSuchElementException
		 */
		public LineInfo nextLine() 
		{
			return (LineInfo)iterator.next();
		}
		
		public void check(LineWalker lw) {
			Iterator it = lw.lines.iterator(); 
			Iterator it2 = lines.iterator(); 
			if(lw.lines.size() != lines.size()) {
				System.out.println("ERROR1");
				return;
			}
			while(it.hasNext()) {
				LineInfo l1 = (LineInfo)it.next();
				LineInfo l2 = (LineInfo)it2.next();
				
				if(!l1.data.equals(l2.data)) {
					System.out.println("ERROR2");
					return;
				}
				if(l1.offset != l2.offset) {
					System.out.println("ERROR3");
					return;
				}
				if(l1.dataOffset != l2.dataOffset) {
					System.out.println("ERROR4");
					return;
				}
				if(l1.posIndex1 != l2.posIndex1) {
					System.out.println("ERROR5");
					return;
				}
			}
		}
	}
	/**
	 * Helper class to handle line information. The text input is partitioned into individual 
	 * lines such that:
	 * <ul>
	 * <li>any leading newline is discarded</li>
	 * <li>all following lines are added, even if empty</li>
	 * <li>if non-whitespace text remains after the last newline, it is trimmed and added</li>
	 * </ul>
	 * The lines are managed as LineInfo objects, which also track the starting offset of the
	 * line data into the underlying document.
	 */
	private static class LineWalker
	{
		private List lines = new ArrayList();
		private Iterator iterator;
		
		public LineWalker(IDocument document, IRegion region, int[] positions) 
			throws BadLocationException
		{
			ILineTracker lineTracker = new DefaultLineTracker();
			
			int textOffset = region.getOffset();
			String text = document.get(textOffset, region.getLength());
			lineTracker.set(text);
			
			int lineCount = lineTracker.getNumberOfLines();
			for(int i=0; i<lineCount; i++) {
				int off = lineTracker.getLineOffset(i);
				String delimiter = lineTracker.getLineDelimiter(i);
				int length = lineTracker.getLineLength(i);
				
				if(delimiter != null) {
					if(i > 0 || length > delimiter.length()) {
						LineInfo info = new LineInfo(
							textOffset+off, text.substring(off, off+length-delimiter.length()), delimiter.length());
						info.recordPositionIndex(positions);
						lines.add(info);
					}
				}
				else {
					//only the add last line if it contains non-whitespace text
					LineInfo info = new LineInfo(textOffset+off, text.substring(off, off+length), 0);
					info.trimData();
					if(info.data.length() > 0) {
						info.recordPositionIndex(positions);
						lines.add(info);
					}
				}
			}
			iterator = lines.iterator();
		}

		/**
		 * @return whether there are any more lines
		 */
		public boolean hasMoreLines() 
		{
			return iterator.hasNext();
		}

		/**
		 * @return the next LineInfo
		 * @throws java.util.NoSuchElementException
		 */
		public LineInfo nextLine() 
		{
			return (LineInfo)iterator.next();
		}
	}
	
	private boolean fUseTabIndent = false;
	private int fTabSpaces = 4;
	private boolean fPreserveNewline = true;

	/* 
	 * run variables reset for every invocation. Defined as state variables
	 * so we dont need to pass them around all the time. Note that this makes the 
	 * formatter non-threadsafe (which is perfectly OK)
	 */
	private IDocument fDocument;
	private int fOffset;
	private String fLineDelimiter;
	private int fInitialIndent;
	private int fIndentLevel;
	private int[] fPositions;
	private List fLineInfos;
	
	/*
	 * @see XSContentFormatter.FormattingStrategy#format(IDocument, int, int, int[])
	 */
	public String format(IDocument document, int offset, int length, int[] positions) 
	{
		try {
			fDocument = document;
			fOffset = offset;
			fLineDelimiter = getLineDelimiter(fDocument);
			fInitialIndent = 0;
			fIndentLevel = 0;
			fPositions = positions;
			fLineInfos = new ArrayList();
			
			return doFormat(length);
		} 
		catch (BadLocationException e) {
			e.printStackTrace(); //shouldnt happen
			return null;
		}
		finally {
			fDocument = null; //release to GC
			fPositions = null;
			fLineInfos = null;
		}
	}
	
	/**
	 * do the actual formatting, after all run variables have been initialized
	 * @param length
	 * @return the ready formatted content string
	 * @throws BadLocationException
	 */
	private String doFormat(int length) throws BadLocationException
	{
		//determine the enclosing element and the appropriate indent
		PartitionWalker walker = new PartitionWalker(fDocument, fOffset);
		
		for(ITypedRegion region = walker.previous(); region != null; region = walker.previous()) {
			if(region.getType() == XMLPartitionScanner.XML_STARTTAG) {
				fInitialIndent = getIndent(region.getOffset());
				if(!isTagClosed(region))
					fIndentLevel++;
				break; 
			}
			else if(region.getType() == XMLPartitionScanner.XML_ENDTAG) {
				fInitialIndent = getIndent(region.getOffset());
				break;
			}
		}
		
		//walk through the partitions and format
		walker = new PartitionWalker(fDocument, fOffset, length);
		StringBuffer buffer = new StringBuffer();

		ITypedRegion region = walker.next();
		while(region != null) {
			if(region.getType() == XMLPartitionScanner.XML_STARTTAG) {
				formatStartTag(region, buffer);
				if(!isTagClosed(region))
					fIndentLevel++;
			}
			else if(region.getType() == XMLPartitionScanner.XML_ENDTAG) {
				if(fIndentLevel > 0)
					fIndentLevel--;
				formatDefault(region, buffer);
			}
			else if(region.getType() == XMLPartitionScanner.XML_CDATA) {
				formatCDATA(region, buffer);
			}
			else if(region.getType() == XMLPartitionScanner.XML_COMMENT) {
				formatDefault(region, buffer);
			}
			else if(region.getType() == XMLPartitionScanner.XML_DEFAULT) {
				formatDefault(region, buffer);
			}
			region = walker.next();
		}
		
		//finally, have the line infos update the positions array
		Iterator it = fLineInfos.iterator();
		while(it.hasNext()) {
			LineInfo info = (LineInfo)it.next();
			info.updatePositions();
		}
		
		return buffer.toString();
	}

	/**
	 * default formatting. Everything is aligned, one indent level above the nearest enclosing 
	 * opening element, if any 
	 */
	private void formatDefault(ITypedRegion region, StringBuffer buffer) 
		throws BadLocationException 
	{	
		LineWalker lineWalker = new LineWalker(fDocument, region, fPositions);
		
		while(lineWalker.hasMoreLines()) {
			LineInfo info = lineWalker.nextLine();
			String line = info.trimData();
			
			if (line.length() > 0) {
				int off = writeLine(info.data, fInitialIndent, fIndentLevel, buffer);

				info.setWriteOffset(off);
				fLineInfos.add(info);
			} 
			else if (fPreserveNewline) {
				buffer.append(fLineDelimiter);
			}
		}
	}
	
	/**
	 * format a start tag. Attributes, if starting on a new line, are given an 
	 * additional indent 
	 */
	private void formatStartTag(ITypedRegion region, StringBuffer buffer) 
		throws BadLocationException 
	{	
		LineWalker lineWalker = new LineWalker(fDocument, region, fPositions);
		
		int count = 0;
		while(lineWalker.hasMoreLines()) {
			LineInfo info = lineWalker.nextLine();
			String line = info.trimData();

			if (line.length() > 0) {
				int indentLevel = count >  0 ? fIndentLevel + 1 : fIndentLevel; 
				int writeOffset = writeLine(info.data, fInitialIndent, indentLevel, buffer);

				info.setWriteOffset(writeOffset);
				fLineInfos.add(info);

				count++;
			} 
		}
	}
	
	/**
	 * format a CDATA region, preserving indenting within the CDATA 
	 */
	private void formatCDATA(ITypedRegion region, StringBuffer buffer) 
		throws BadLocationException 
	{	
		LineWalker lineWalker = new LineWalker(fDocument, region, fPositions);
		
		LineInfo info = lineWalker.nextLine();
		int firstIndent = getIndent(info.offset);
		info.trimData();
		int writeOffset = writeLine(info.data, fInitialIndent, fIndentLevel, buffer);

		info.setWriteOffset(writeOffset);
		fLineInfos.add(info);
		
		while(lineWalker.hasMoreLines()) {
			info = lineWalker.nextLine();
			int indentDelta = getIndent(info.data) - firstIndent;
			String line = info.trimData();
			if(line.length() > 0) {
				int indent = indentDelta > 0 ? fInitialIndent + indentDelta : fInitialIndent;
				writeOffset = writeLine(info.data, indent, fIndentLevel, buffer);

				info.setWriteOffset(writeOffset);
				fLineInfos.add(info);
			}
			else {
				buffer.append(fLineDelimiter);
			}
		}
	}
	
	/**
	 * @param region a region of type START_TAG
	 * @return whether the given region is terminated by a closing tag marker ("/>").
	 */
	private boolean isTagClosed(ITypedRegion region) throws BadLocationException 
	{
		char c = fDocument.getChar(region.getOffset()+region.getLength()-2);
		return (c == '/');
	}

	/**
	 * Embodies the policy which line delimiter to use when inserting into
	 * a document.<br>
	 * <em>Copied from org.eclipse.jdt.internal.corext.codemanipulation.StubUtility</em>
	 */	
	private String getLineDelimiter(IDocument document) 
	{
		// new for: 1GF5UU0: ITPJUI:WIN2000 - "Organize Imports" in java editor inserts lines in wrong format
		String lineDelim= null;
		try {
			lineDelim= document.getLineDelimiter(0);
		} catch (BadLocationException e) {
		}
		if (lineDelim == null) {
			String systemDelimiter= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			String[] lineDelims = document.getLegalLineDelimiters();
			for (int i= 0; i < lineDelims.length; i++) {
				if (lineDelims[i].equals(systemDelimiter)) {
					lineDelim= systemDelimiter;
					break;
				}
			}
			if (lineDelim == null) {
				lineDelim= lineDelims.length > 0 ? lineDelims[0] : systemDelimiter;
			}
		}
		return lineDelim;
	}

	/**
	 * Returns the indentation of the line of the given offset.
	 *
	 * @param offset the offset
	 * @return the indentation of the line of the offset
	 */
	private int getIndent(int offset) {
		
		try {
			int start= fDocument.getLineOfOffset(offset);
			start= fDocument.getLineOffset(start);
			
			int count = 0;
			for(int i=start; i<fDocument.getLength(); ++i) {
				char c = fDocument.getChar(i);
				if('\t' == c)
					count += fTabSpaces;
				else if(' ' == c)
					count++;
				else break;
			}
			return count;
		} 
		catch (BadLocationException x) {
			return 0;
		}
	}

	/**
	 * @param line
	 * @return the number of character positions 
	 */
	private int getIndent(String line) 
	{
		int count=0;
		for(int i=0; i<line.length(); ++i) {
			char c=line.charAt(i);
			if('\t' == c)
				count += fTabSpaces;
			else if(' ' == c)
				count++;
			else break;
		}
		return count;
	}
	
	/**
	 * Write the line data to the given buffer, using the appropriate indent. Terminate 
	 * with a line delimiter. 
	 * 
	 * @param initialColumns initial columns to indent. Tabs or spaces, depending on preference
	 * @param indentLevel the number of indents (tabs or spaces) to add 
	 * @param buffer the buffer to write to
	 */
	private int writeLine(String line, int initialColumns, int indentLevel, StringBuffer buffer) 
	{
		int writeOffset = fOffset + buffer.length();
		writeOffset += writeColumns(initialColumns, buffer);
		writeOffset += writeIndent(indentLevel, buffer);
			
		buffer.append(line);
		buffer.append(fLineDelimiter);
		
		return writeOffset;
	}

	/**
	 * write a given number of whitespace columns, using tabs or spaces depending on preference
	 * @param columnCount the columns to write
	 * @param buffer the buffer to write to
	 * @return the number of characters inserted into the buffer
	 */
	private int writeColumns(int columnCount, StringBuffer buffer)
	{
		if(fUseTabIndent) {
			int tabs = columnCount / fTabSpaces;
			int spaces = columnCount % fTabSpaces;

			for(int i=0; i<tabs; i++)
				buffer.append('\t');
			for(int i=0; i<spaces; i++)
				buffer.append(' ');
				
			return tabs + spaces;
		}
		else {
			for(int i=0; i<columnCount; i++)
				buffer.append(' ');
				
			return columnCount;
		}
	}

	/**
	 * write a given number of whitespace columns, using tabs or spaces depending on preference
	 * @param indentCount the number of indents to write
	 * @param buffer the buffer to write to
	 * @return the number of characters inserted into the buffer
	 */
	private int writeIndent(int indentCount, StringBuffer buffer)
	{
		if(fUseTabIndent) {
			for(int i=0; i<indentCount; i++)
				buffer.append('\t');
				
			return indentCount;
		}
		else {
			int length = indentCount * fTabSpaces;
			for(int i=0; i<length; i++)
				buffer.append(' ');

			return length;
		}
	}
	/**
	 * @return whether empty lines should be preserved during formatting (default: true)
	 */
	public boolean isPreserveEmpty() {
		return fPreserveNewline;
	}

	/**
	 * determine the number of spaces to use if tabs are not used (default: 4)
	 * @return the number of spaces per tab
	 */
	public int getTabSpaces() {
		return fTabSpaces;
	}

	/**
	 * @return whether indenting should be done by tabs (default: true).
	 * This will also replace any existing space indents.
	 */
	public boolean isUseTabIndent() {
		return fUseTabIndent;
	}

	/**
	 * determine whether empty lines should be preserved during formatting (default: true)
	 * @param preserve true if lines should be preserved
	 */
	public void setPreserveEmpty(boolean preserve) {
		fPreserveNewline = preserve;
	}

	/**
	 * determine the number of spaces to use if tabs are not used (default: 4)
	 * @param spaces the number of spaces per tab
	 */
	public void setTabSpaces(int spaces) {
		fTabSpaces = spaces;
	}

	/**
	 * Determine whether indenting should be done by tabs (default: true).
	 * This will also replace any existing space indents.
	 * 
	 * @param <code>true</code> if tabs should be used for indenting. 
	 */
	public void setUseTabIndent(boolean useTabs) {
		fUseTabIndent = useTabs;
	}
}
