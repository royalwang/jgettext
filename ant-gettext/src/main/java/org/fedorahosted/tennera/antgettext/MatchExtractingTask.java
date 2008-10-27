/*
 * JBoss, the OpenSource J2EE webOS
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.fedorahosted.tennera.antgettext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.jboss.jgettext.Catalog;
import org.jboss.jgettext.Message;
import org.jboss.jgettext.Occurence;
import org.jboss.jgettext.catalog.write.CatalogWriter;

/**
 * Extracts strings which match some condition into a gettext template file (POT).
 * 
 * @author <a href="sflaniga@redhat.com">Sean Flanigan</a>
 * @version $Revision: 1.1 $
 */
public abstract class MatchExtractingTask extends MatchingTask 
{
    private BufferedWriter out;
    private File srcDir;
    private File target;
    private String pathPrefix;
    private String format = "";
    
    /**
     * Maps an English key to a set of matching source locations
     */
    private Map<String, Set<String>> mapKeyToLocationSet = new TreeMap<String, Set<String>>();

    /**
     * Finds the (1-based) number of the line containing a character, 
     * specified by its character index from the beginning of the file.
     * @param lineStarts
     * @param charNo
     * @return
     */
    protected static int findLineNumber(Integer[] lineStarts, int charNo) 
    {
	// Find the largest index in the list less than or equal to charNo, and add 1.  
	int position = Arrays.binarySearch(lineStarts, charNo);
	if (position >= 0)
	    return position + 1;
	else
	    return -position - 1;
    }

    public void setSrcDir(File srcDir) 
    {
	this.srcDir = srcDir;
    }

    public void setTarget(File target) 
    {
	this.target = target;
    }

    public void setPathPrefix(String prefix) 
    {
	this.pathPrefix = prefix;
    }
    
    public void setFormat(String format) 
    {
	this.format = format;
    }

    @Override
    public void execute() throws BuildException 
    {
	DirUtil.checkDir(srcDir, "srcDir", false);
	//      if (target == null)
	//      {
	//         throw new BuildException("target attribute must be set!");
	//      }
	if (target != null && target.exists() && !target.isFile())
	{
	    throw new BuildException("target exists but is not a file!");
	}

	try
	{
	    if(target == null)
	    {
		log("Extracting English strings from '" +srcDir+ "' to STDOUT");
		out = new BufferedWriter(new OutputStreamWriter(System.out));
	    }
	    else
	    {
		log("Extracting English strings from '" +srcDir+ "' to '"+target+"'");
		out = new BufferedWriter(new FileWriter(target));
	    }
	    DirectoryScanner ds = this.getDirectoryScanner(srcDir);
	    String[] files = ds.getIncludedFiles();

	    for (int i = 0; i < files.length; i++)
	    {
		String filename = files[i];
		log("processing " + filename, Project.MSG_VERBOSE);
		File f = new File(srcDir, filename);
		processFile(filename, f);
	    }
	    generatePot(out);
	    out.close();
	}
	catch (IOException e)
	{
	    throw new BuildException(e);
	}
    }

    /**
     * Scans a single file for matches, recording them for later output.  
     * @param filename
     * @throws IOException
     * @throws BuildException
     */
    protected abstract void processFile(String filename, File f) throws IOException; 

    /**
     * Records a match for later output by generatePot
     * @param filename
     * @param key
     * @param lineNo
     */
    protected void recordMatch(String filename, String key, int lineNo) 
    {
	Set<String> set = mapKeyToLocationSet.get(key);
	if(set == null)
	{
	    set = new TreeSet<String>();
	    mapKeyToLocationSet.put(key, set);
	}
	set.add(filename+":"+lineNo); //$NON-NLS-1$
    }

    private void generatePot(BufferedWriter out) throws IOException 
    {
	Catalog cat = new Catalog(true);
	CatalogWriter writer = new CatalogWriter(cat);
	for (Map.Entry<String, Set<String>> mapEntry : mapKeyToLocationSet.entrySet())
	{
	    Message message = new Message();
	    //	   message.addExtractedComment(null);
	    message.addOccurence(new Occurence(locationSetToString(mapEntry.getValue())));
	    if (!format.equals(""))
		message.addFormat(format);
	    //	   message.setMsgctxt(null);
	    message.setMsgid(mapEntry.getKey());
	    cat.addMessage(message);
	}
	writer.writeTo(out);
    }

    private String locationSetToString(Set<String> locationSet) 
    {
	StringBuilder sb = new StringBuilder();
	for (String location : locationSet)
	{
	    sb.append(pathPrefix);
	    sb.append(location);
	    // TODO don't use a space after last element
	    sb.append(' ');
	}
	return sb.toString();
    }

}