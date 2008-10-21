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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.jboss.jgettext.Catalog;
import org.jboss.jgettext.Message;
import org.jboss.jgettext.Catalog.MessageProcessor;
import org.jboss.jgettext.catalog.parse.ExtendedCatalogParser;
import org.jboss.jgettext.catalog.write.CatalogWriter;

/**
 * 
 * 
 * @author <a href="sflaniga@redhat.com">Sean Flanigan</a>
 * @version $Revision: $
 */
public class Pot2EnTask extends MatchingTask
{
   private File srcDir;
   private File dstDir;
   private boolean pseudo;

   public void setSrcDir(File srcDir)
   {
      this.srcDir = srcDir;
   }

   public void setDstDir(File dstDir)
   {
      this.dstDir = dstDir;
   }
   
   public void setPseudo(boolean munge)
   {
       this.pseudo = munge;
   }
   
   @Override
   public void execute() throws BuildException
   {
      DirUtil.checkDir(srcDir, "srcDir", false);
      DirUtil.checkDir(dstDir, "dstDir", true);

      try
      {
         DirectoryScanner ds = super.getDirectoryScanner(srcDir);
         // use default includes if unset:
         if(!getImplicitFileSet().hasPatterns())
             ds.setIncludes(new String[] {"**/*.pot"});
         ds.scan();
         String[] files = ds.getIncludedFiles();

         for (int i = 0; i < files.length; i++)
         {
            String potFilename = files[i];
            File potFile = new File(srcDir, potFilename);
            String poFilename = potFilename.substring(0, potFilename.length()-"pot".length())+"po";
            File poFile = new File(dstDir, poFilename);
            
            log("Generating "+poFile+" from "+potFile, Project.MSG_VERBOSE);
            poFile.getParentFile().mkdirs();
            BufferedWriter out = new BufferedWriter(new FileWriter(poFile));
            try
            {
//               String comment = poFilename+" generated by "+Pot2EnTask.class.getName()+" from "+potFilename;
        	ExtendedCatalogParser parser = new ExtendedCatalogParser( potFile );
        	parser.catalog();
        	Catalog catalog = parser.getCatalog();
        	catalog.setTemplate(false);
//        	String header = "Header: "+catalog.locateHeader();
//		System.out.println(header);
        	MessageProcessor processor = new MessageProcessor() 
        	{
        	    public void processMessage(Message entry) 
        	    {
        		if (!entry.isHeader()) 
        		{
			    String msgid = entry.getMsgid();
			    if (pseudo)
				entry.setMsgstr(StringUtil.pseudolocalise(msgid));
			    else
				entry.setMsgstr(msgid);
			}
        	    }
        	};
        	catalog.processMessages(processor);
		CatalogWriter writer = new CatalogWriter(catalog);
		writer.writeTo(poFile);
            }
            finally
            {
               out.close();
            }
         }
      }
      catch (Exception e)
      {
         throw new BuildException(e);
      }
   }
   
}
