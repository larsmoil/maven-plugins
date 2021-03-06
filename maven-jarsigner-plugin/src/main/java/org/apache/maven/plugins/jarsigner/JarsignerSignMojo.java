package org.apache.maven.plugins.jarsigner;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Signs a project artifact and attachments using jarsigner.
 *
 * @author <a href="cs@schulte.it">Christian Schulte</a>
 * @version $Id$
 * @goal sign
 * @phase package
 * @since 1.0
 */
public class JarsignerSignMojo
    extends AbstractJarsignerMojo
{

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.keystore}"
     */
    private String keystore;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.storepass}"
     */
    private String storepass;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.keypass}"
     */
    private String keypass;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.sigfile}"
     */
    private String sigfile;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.storetype}"
     */
    private String storetype;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     * 
     * @parameter expression="${jarsigner.providerName}"
     */
    private String providerName;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     * 
     * @parameter expression="${jarsigner.providerClass}"
     */
    private String providerClass;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     * 
     * @parameter expression="${jarsigner.providerArg}"
     */
    private String providerArg;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.alias}"
     * @required
     */
    private String alias;

    /**
     * Indicates whether existing signatures should be removed from the processed JAR files prior to signing them. If
     * enabled, the resulting JAR will appear as being signed only once.
     * 
     * @parameter expression="${jarsigner.removeExistingSignatures}" default-value="false"
     * @since 1.1
     */
    private boolean removeExistingSignatures;

    protected Commandline getCommandline( final File archive, final Commandline commandLine )
    {
        if ( archive == null )
        {
            throw new NullPointerException( "archive" );
        }
        if ( commandLine == null )
        {
            throw new NullPointerException( "commandLine" );
        }

        if ( !StringUtils.isEmpty( this.keystore ) )
        {
            commandLine.createArg().setValue( "-keystore" );
            commandLine.createArg().setValue( this.keystore );
        }
        if ( !StringUtils.isEmpty( this.storepass ) )
        {
            commandLine.createArg().setValue( "-storepass" );
            commandLine.createArg().setValue( this.storepass );
        }
        if ( !StringUtils.isEmpty( this.keypass ) )
        {
            commandLine.createArg().setValue( "-keypass" );
            commandLine.createArg().setValue( this.keypass );
        }
        if ( !StringUtils.isEmpty( this.storetype ) )
        {
            commandLine.createArg().setValue( "-storetype" );
            commandLine.createArg().setValue( this.storetype );
        }
        if ( !StringUtils.isEmpty( this.providerName ) )
        {
            commandLine.createArg().setValue( "-providerName" );
            commandLine.createArg().setValue( this.providerName );
        }
        if ( !StringUtils.isEmpty( this.providerClass ) )
        {
            commandLine.createArg().setValue( "-providerClass" );
            commandLine.createArg().setValue( this.providerClass );
        }
        if ( !StringUtils.isEmpty( this.providerArg ) )
        {
            commandLine.createArg().setValue( "-providerArg" );
            commandLine.createArg().setValue( this.providerArg );
        }
        if ( !StringUtils.isEmpty( this.sigfile ) )
        {
            commandLine.createArg().setValue( "-sigfile" );
            commandLine.createArg().setValue( this.sigfile );
        }

        commandLine.createArg().setFile( archive );

        if ( !StringUtils.isEmpty( this.alias ) )
        {
            commandLine.createArg().setValue( this.alias );
        }

        return commandLine;
    }

    protected String getCommandlineInfo( final Commandline commandLine )
    {
        String commandLineInfo = commandLine != null ? commandLine.toString() : null;

        if ( commandLineInfo != null )
        {
            commandLineInfo = StringUtils.replace( commandLineInfo, this.keypass, "'*****'" );
            commandLineInfo = StringUtils.replace( commandLineInfo, this.storepass, "'*****'" );
        }

        return commandLineInfo;
    }

    protected void preProcessArchive( final File archive )
        throws MojoExecutionException
    {
        if ( removeExistingSignatures )
        {
            unsignArchive( archive );
        }
    }

    /**
     * Removes any existing signatures from the specified JAR file. We will stream from the input JAR directly to the
     * output JAR to retain as much metadata from the original JAR as possible.
     * 
     * @param jarFile The JAR file to unsign, must not be <code>null</code>.
     * @throws MojoExecutionException If the unsigning failed.
     */
    private void unsignArchive( final File jarFile )
        throws MojoExecutionException
    {
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Unsigning " + jarFile );
        }

        File unsignedFile = new File( jarFile.getAbsolutePath() + ".unsigned" );

        ZipInputStream zis = null;
        ZipOutputStream zos = null;
        try
        {
            zis = new ZipInputStream( new BufferedInputStream( new FileInputStream( jarFile ) ) );
            zos = new ZipOutputStream( new BufferedOutputStream( new FileOutputStream( unsignedFile ) ) );

            for ( ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry() )
            {
                if ( isSignatureFile( ze.getName() ) )
                {
                    if ( getLog().isDebugEnabled() )
                    {
                        getLog().debug( "  Removing " + ze.getName() );
                    }

                    continue;
                }

                zos.putNextEntry( ze );

                IOUtil.copy( zis, zos );
            }

        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to unsign archive " + jarFile + ": " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( zis );
            IOUtil.close( zos );
        }

        try
        {
            FileUtils.rename( unsignedFile, jarFile );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to unsign archive " + jarFile + ": " + e.getMessage(), e );
        }
    }

    /**
     * Checks whether the specified JAR file entry denotes a signature-related file, i.e. matches
     * <code>META-INF/*.SF</code>, <code>META-INF/*.DSA</code> or <code>META-INF/*.RSA</code>.
     * 
     * @param entryName The name of the JAR file entry to check, must not be <code>null</code>.
     * @return <code>true</code> if the entry is related to a signature, <code>false</code> otherwise.
     */
    private boolean isSignatureFile( String entryName )
    {
        if ( entryName.regionMatches( true, 0, "META-INF", 0, 8 ) )
        {
            entryName = entryName.replace( '\\', '/' );

            if ( entryName.indexOf( '/' ) == 8 && entryName.lastIndexOf( '/' ) == 8 )
            {
                if ( entryName.regionMatches( true, entryName.length() - 3, ".SF", 0, 3 ) )
                {
                    return true;
                }
                if ( entryName.regionMatches( true, entryName.length() - 4, ".DSA", 0, 4 ) )
                {
                    return true;
                }
                if ( entryName.regionMatches( true, entryName.length() - 4, ".RSA", 0, 4 ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

}
