package org.apache.maven.plugin.assembly.archive;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.archiver.ArchiveFinalizer;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.FinalizerEnabled;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.collections.ActiveCollectionManager;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

public class DefaultAssemblyArchiverTest
    extends PlexusTestCase
{

    private TestFileManager fileManager = new TestFileManager( "def-assy-archiver.test.", "" );

    public void tearDown()
        throws Exception
    {
        fileManager.cleanUp();
        super.tearDown();
    }

    public void testCreateArchive()
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException,
        IOException
    {
        MockManager mm = new MockManager();

        MockAndControlForAssemblyArchiver macMgr = new MockAndControlForAssemblyArchiver( mm );

        macMgr.expectGetArchiver( "zip", Archiver.class );
        macMgr.expectGetDestFile( new File( "test" ) );

        MockControl phaseControl = MockControl.createControl( AssemblyArchiverPhase.class );
        mm.add( phaseControl );

        AssemblyArchiverPhase phase = (AssemblyArchiverPhase) phaseControl.getMock();

        phase.execute( null, null, null, null );
        phaseControl.setMatcher( MockControl.ALWAYS_MATCHER );

        MockControl csControl = MockControl.createControl( AssemblerConfigurationSource.class );
        mm.add( csControl );

        AssemblerConfigurationSource configSource = (AssemblerConfigurationSource) csControl.getMock();

        File tempDir = fileManager.createTempDir();
        FileUtils.deleteDirectory( tempDir );

        configSource.getTemporaryRootDirectory();
        csControl.setReturnValue( tempDir, MockControl.ZERO_OR_MORE );

        configSource.isDryRun();
        csControl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        configSource.isIgnoreDirFormatExtensions();
        csControl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        File outDir = fileManager.createTempDir();

        macMgr.archiver.setDestFile( new File( outDir, "full-name.zip" ) );

        try
        {
            macMgr.archiver.createArchive();
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen" );
        }
        catch ( IOException e )
        {
            fail( "Should never happen" );
        }

        configSource.getOutputDirectory();
        csControl.setReturnValue( outDir );

        configSource.getFinalName();
        csControl.setReturnValue( "finalName" );
        
        configSource.getArchiverConfig();
        csControl.setReturnValue( null, MockControl.ZERO_OR_MORE );

        Assembly assembly = new Assembly();

        mm.replayAll();

        DefaultAssemblyArchiver subject = createSubject( macMgr, Collections.singletonList( phase ), null );

        subject.createArchive( assembly, "full-name", "zip", configSource );

        mm.verifyAll();
    }

    public void testCreateArchiver_ShouldConfigureArchiver()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        TestArchiverWithConfig archiver = new TestArchiverWithConfig();

        macArchiverManager.expectGetArchiver( "dummy", archiver );

        MockControl configCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        AssemblerConfigurationSource configSource = (AssemblerConfigurationSource) configCtl.getMock();
        
        String simpleConfig = "value";
        
        configSource.getArchiverConfig();
        configCtl.setReturnValue( "<configuration><simpleConfig>" + simpleConfig + "</simpleConfig></configuration>", MockControl.ZERO_OR_MORE );
        
        MavenProject project = new MavenProject( new Model() );
        
        configSource.getProject();
        configCtl.setReturnValue( project, MockControl.ZERO_OR_MORE );
        
        configSource.getMavenSession();
        configCtl.setReturnValue( null, MockControl.ZERO_OR_MORE );
        
        configSource.isDryRun();
        configCtl.setReturnValue( false, MockControl.ZERO_OR_MORE );
        
        mm.add( configCtl );

        mm.replayAll();

        DefaultAssemblyArchiver subject = createSubject( macArchiverManager, Collections.EMPTY_LIST, null );

        subject.createArchiver( "dummy", false, "finalName", configSource, null );
        
        assertEquals( simpleConfig, archiver.getSimpleConfig() );

        mm.verifyAll();
    }

    public void testCreateArchiver_ShouldCreateTarArchiverWithNoCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        TestTarArchiver ttArchiver = new TestTarArchiver();

        MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        MockControl configCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        AssemblerConfigurationSource configSource = (AssemblerConfigurationSource) configCtl.getMock();

        configSource.getTarLongFileMode();
        configCtl.setReturnValue( TarLongFileMode.FAIL, MockControl.ZERO_OR_MORE );

        configSource.isDryRun();
        configCtl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        configSource.getArchiverConfig();
        configCtl.setReturnValue( null, MockControl.ZERO_OR_MORE );
        
        configSource.getProject();
        configCtl.setReturnValue( new MavenProject( new Model() ), MockControl.ZERO_OR_MORE );

        configSource.getJarArchiveConfiguration();
        configCtl.setReturnValue( null, MockControl.ZERO_OR_MORE );
        
        mm.add( configCtl );

        mm.replayAll();

        DefaultAssemblyArchiver subject = createSubject( macArchiverManager, Collections.EMPTY_LIST, null );

        subject.createArchiver( "tar", false, "finalName", configSource, null );

        assertNull( ttArchiver.compressionMethod );
        assertEquals( TarLongFileMode.FAIL, ttArchiver.longFileMode.getValue() );

        mm.verifyAll();
    }

    public void testCreateArchiver_ShouldCreateWarArchiverWithIgnoreWebxmlSetToFalse()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        TestWarArchiver twArchiver = new TestWarArchiver();

        MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "war", twArchiver );

        MockControl configCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        AssemblerConfigurationSource configSource = (AssemblerConfigurationSource) configCtl.getMock();

        configSource.isDryRun();
        configCtl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        configSource.getArchiverConfig();
        configCtl.setReturnValue( null, MockControl.ZERO_OR_MORE );

        configSource.getProject();
        configCtl.setReturnValue( new MavenProject( new Model() ), MockControl.ZERO_OR_MORE );
        
        configSource.getJarArchiveConfiguration();
        configCtl.setReturnValue( null, MockControl.ZERO_OR_MORE );
        
        mm.add( configCtl );
        
        mm.replayAll();

        DefaultAssemblyArchiver subject = createSubject( macArchiverManager, Collections.EMPTY_LIST, null );

        subject.createArchiver( "war", false, null, configSource, null );

        assertFalse( twArchiver.ignoreWebxml );
    }

    public void testCreateArchiver_ShouldCreateZipArchiver()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        ZipArchiver archiver = new ZipArchiver();

        MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "zip", archiver );

        MockControl configCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        AssemblerConfigurationSource configSource = (AssemblerConfigurationSource) configCtl.getMock();

        configSource.isDryRun();
        configCtl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        configSource.getArchiverConfig();
        configCtl.setReturnValue( null, MockControl.ZERO_OR_MORE );
        
        mm.add( configCtl );

        mm.replayAll();

        DefaultAssemblyArchiver subject = createSubject( macArchiverManager, Collections.EMPTY_LIST, null );

        subject.createArchiver( "zip", false, null, configSource, null );
    }

    // TODO: Re-implement these tests on the createArchiver(..) method. For now, they're no big loss.
    // public void testConfigureArchiverFinalizers_ShouldDoNothingWhenNotSupportedByArchiver()
    // {
    // MockManager mm = new MockManager();
    //
    // MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );
    //
    // macArchiverManager.createArchiver( Archiver.class );
    //
    // ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();
    //
    // mm.replayAll();
    //
    // DefaultAssemblyArchiver subject =
    // createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );
    //
    // subject.configureArchiverFinalizers( macArchiverManager.archiver, "format", null, filter );
    //
    // mm.verifyAll();
    // }
    //
    // public void testConfigureArchiverFinalizers_ShouldAddManifestFinalizerWhenSupportedByArchiver()
    // {
    // MockManager mm = new MockManager();
    //
    // MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );
    //
    // macArchiverManager.createArchiver( TestFinalizerFilteredArchiver.class );
    //
    // Set finalizerClasses = new HashSet();
    // finalizerClasses.add( ComponentsXmlArchiverFileFilter.class );
    // finalizerClasses.add( ManifestCreationFinalizer.class );
    //
    // macArchiverManager.expectSetArchiverFinalizers( finalizerClasses );
    //
    // ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();
    //
    // MockControl configCtl = MockControl.createControl( AssemblerConfigurationSource.class );
    // AssemblerConfigurationSource configSource = (AssemblerConfigurationSource) configCtl.getMock();
    //
    // Model model = new Model();
    // model.setGroupId( "group" );
    // model.setArtifactId( "artifact" );
    // model.setVersion( "1" );
    //
    // configSource.getProject();
    // configCtl.setReturnValue( new MavenProject( model ), MockControl.ZERO_OR_MORE );
    //
    // configSource.getJarArchiveConfiguration();
    // configCtl.setReturnValue( new MavenArchiveConfiguration() );
    //
    // mm.add( configCtl );
    //
    // mm.replayAll();
    //
    // DefaultAssemblyArchiver subject =
    // createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );
    //
    // subject.configureArchiverFinalizers( macArchiverManager.archiver, "jar", configSource, filter );
    //
    // mm.verifyAll();
    // }
    //
    // public void testConfigureArchiverFilters_ShouldDoNothingWhenNotSupportedByArchiver()
    // {
    // MockManager mm = new MockManager();
    //
    // MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );
    //
    // macArchiverManager.createArchiver( Archiver.class );
    //
    // ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();
    //
    // mm.replayAll();
    //
    // DefaultAssemblyArchiver subject =
    // createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );
    //
    // subject.configureArchiverFilters( macArchiverManager.archiver, filter );
    //
    // mm.verifyAll();
    // }
    //
    // public void testConfigureArchiverFilters_ShouldAddComponentsFilterWhenSupportedByArchiver()
    // {
    // MockManager mm = new MockManager();
    //
    // MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );
    //
    // macArchiverManager.createArchiver( TestFinalizerFilteredArchiver.class );
    // macArchiverManager.expectSetArchiverFilters();
    //
    // ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();
    //
    // mm.replayAll();
    //
    // DefaultAssemblyArchiver subject =
    // createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );
    //
    // subject.configureArchiverFilters( macArchiverManager.archiver, filter );
    //
    // mm.verifyAll();
    // }

    public void testCreateWarArchiver_ShouldDisableIgnoreWebxmlOption()
        throws NoSuchArchiverException
    {
        MockManager mm = new MockManager();

        TestWarArchiver twArchiver = new TestWarArchiver();

        MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "war", twArchiver );

        mm.replayAll();

        DefaultAssemblyArchiver subject = createSubject( macArchiverManager, Collections.EMPTY_LIST, null );

        subject.createWarArchiver();

        assertFalse( twArchiver.ignoreWebxml );
    }

    public void testCreateTarArchiver_ShouldNotInitializeCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        TestTarArchiver ttArchiver = new TestTarArchiver();

        MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        mm.replayAll();

        DefaultAssemblyArchiver subject = createSubject( macArchiverManager, Collections.EMPTY_LIST, null );

        subject.createTarArchiver( "tar", TarLongFileMode.FAIL );

        assertNull( ttArchiver.compressionMethod );
        assertEquals( TarLongFileMode.FAIL, ttArchiver.longFileMode.getValue() );

        mm.verifyAll();
    }

    public void testCreateTarArchiver_ShouldInitializeGZipCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        TestTarArchiver ttArchiver = new TestTarArchiver();

        MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        mm.replayAll();

        DefaultAssemblyArchiver subject = createSubject( macArchiverManager, Collections.EMPTY_LIST, null );

        subject.createTarArchiver( "tar.gz", TarLongFileMode.FAIL );

        assertEquals( "gzip", ttArchiver.compressionMethod.getValue() );
        assertEquals( TarLongFileMode.FAIL, ttArchiver.longFileMode.getValue() );

        mm.verifyAll();
    }

    public void testCreateTarArchiver_ShouldInitializeBZipCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        TestTarArchiver ttArchiver = new TestTarArchiver();

        MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        mm.replayAll();

        DefaultAssemblyArchiver subject = createSubject( macArchiverManager, Collections.EMPTY_LIST, null );

        subject.createTarArchiver( "tar.bz2", TarLongFileMode.FAIL );

        assertEquals( "bzip2", ttArchiver.compressionMethod.getValue() );
        assertEquals( TarLongFileMode.FAIL, ttArchiver.longFileMode.getValue() );

        mm.verifyAll();
    }

    public void testCreateTarArchiver_ShouldFailWithInvalidCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        TestTarArchiver ttArchiver = new TestTarArchiver();

        MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        mm.replayAll();

        DefaultAssemblyArchiver subject = createSubject( macArchiverManager, Collections.EMPTY_LIST, null );

        try
        {
            subject.createTarArchiver( "tar.Z", null );

            fail( "Invalid compression formats should throw an error." );
        }
        catch ( IllegalArgumentException e )
        {
            // expected.
        }

        mm.verifyAll();
    }

    private DefaultAssemblyArchiver createSubject( MockAndControlForAssemblyArchiver macMgr, List phases, Logger logger )
    {
        DefaultAssemblyArchiver subject =
            new DefaultAssemblyArchiver( macMgr.archiverManager, macMgr.collectionManager, macMgr.resolver, phases );
        
        subject.setContainer( getContainer() );

        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );
        }

        subject.enableLogging( logger );

        return subject;
    }

    private final class MockAndControlForAssemblyArchiver
    {
        ArchiverManager archiverManager;

        MockControl control;

        MockControl archiverControl;

        Archiver archiver;

        MockControl collectionManagerControl;

        ActiveCollectionManager collectionManager;
        
        MockControl resolverControl;
        
        DependencyResolver resolver;

        private final MockManager mm;

        public MockAndControlForAssemblyArchiver( MockManager mm )
        {
            this.mm = mm;
            control = MockControl.createControl( ArchiverManager.class );
            mm.add( control );

            archiverManager = (ArchiverManager) control.getMock();

            collectionManagerControl = MockControl.createControl( ActiveCollectionManager.class );
            mm.add( collectionManagerControl );

            collectionManager = (ActiveCollectionManager) collectionManagerControl.getMock();
            
            resolverControl = MockControl.createControl( DependencyResolver.class );
            mm.add( resolverControl );
            
            resolver = (DependencyResolver) resolverControl.getMock();
            
            try
            {
                resolver.buildManagedVersionMap( null, null );
            }
            catch ( ArtifactResolutionException e )
            {
            }
            catch ( ArchiveCreationException e )
            {
            }
            catch ( InvalidVersionSpecificationException e )
            {
            }
            catch ( InvalidDependencyVersionException e )
            {
            }
            
            resolverControl.setMatcher( MockControl.ALWAYS_MATCHER );
            resolverControl.setReturnValue( Collections.EMPTY_MAP, MockControl.ZERO_OR_MORE );
        }

        void expectGetDestFile( File file )
        {
            archiver.getDestFile();
            archiverControl.setReturnValue( file, MockControl.ZERO_OR_MORE );
        }

        void createArchiver( Class archiverClass )
        {
            archiverControl = MockControl.createControl( archiverClass );
            mm.add( archiverControl );

            archiver = (Archiver) archiverControl.getMock();
        }

        void expectSetArchiverFinalizers()
        {
            ( (FinalizerEnabled) archiver ).setArchiveFinalizers( null );
            archiverControl.setMatcher( MockControl.ALWAYS_MATCHER );
        }

        void expectSetArchiverFinalizers( final Set finalizerClasses )
        {
            ( (FinalizerEnabled) archiver ).setArchiveFinalizers( null );
            archiverControl.setMatcher( new ArgumentsMatcher()
            {

                public boolean matches( Object[] expected, Object[] actual )
                {
                    boolean match = true;
                    List actualClasses = (List) actual[0];

                    Set finClasses = new HashSet( finalizerClasses );

                    for ( Iterator it = actualClasses.iterator(); it.hasNext(); )
                    {
                        ArchiveFinalizer finalizer = (ArchiveFinalizer) it.next();
                        match = match && finClasses.remove( finalizer.getClass() );
                    }

                    return match;
                }

                public String toString( Object[] arguments )
                {
                    return "Matcher for finalizer-classes: " + finalizerClasses;
                }

            } );
        }

        // void expectSetArchiverFilters()
        // {
        // ( ( FilterEnabled ) archiver ).setArchiveFilters( null );
        // archiverControl.setMatcher( MockControl.ALWAYS_MATCHER );
        // }

        void expectGetArchiver( String format, Class archiverClass )
        {
            createArchiver( archiverClass );

            try
            {
                archiverManager.getArchiver( format );
            }
            catch ( NoSuchArchiverException e )
            {
                Assert.fail( "should never happen" );
            }

            control.setReturnValue( archiver );
        }

        void expectGetArchiver( String format, Archiver archiver )
            throws NoSuchArchiverException
        {
            archiverManager.getArchiver( format );
            control.setReturnValue( archiver );
        }
    }

    private static final class TestTarArchiver
        extends TarArchiver
    {

        boolean archiveCreated;

        boolean optionsRetrieved;

        TarCompressionMethod compressionMethod;

        int defaultDirMode;

        int defaultFileMode;

        TarLongFileMode longFileMode;

        TarOptions options;

        protected void execute()
            throws ArchiverException, IOException
        {
            defaultDirMode = getOverrideDirectoryMode();
            defaultFileMode = getOverrideFileMode();
            
            archiveCreated = true;
            super.createArchive();
        }

        public TarOptions getOptions()
        {
            optionsRetrieved = true;
            return super.getOptions();
        }

        public void setCompression( TarCompressionMethod mode )
        {
            compressionMethod = mode;
            super.setCompression( mode );
        }

        public void setLongfile( TarLongFileMode mode )
        {
            longFileMode = mode;
            super.setLongfile( mode );
        }

        public void setOptions( TarOptions options )
        {
            this.options = options;
            super.setOptions( options );
        }

    }

    private static final class TestWarArchiver
        extends WarArchiver
    {

        boolean ignoreWebxml;

        public void setIgnoreWebxml( boolean ignore )
        {
            ignoreWebxml = ignore;
            super.setIgnoreWebxml( ignore );
        }

    }

    interface TestFinalizerFilteredArchiver
        extends Archiver, FinalizerEnabled
    {

    }

    public static final class TestArchiverWithConfig
        implements Archiver
    {
        
        private String simpleConfig;
        
        public void setSimpleConfig( String simpleConfig )
        {
            this.simpleConfig = simpleConfig;
        }
        
        public String getSimpleConfig()
        {
            return simpleConfig;
        }

        public void addArchivedFileSet( File arg0 )
            throws ArchiverException
        {
        }

        public void addArchivedFileSet( ArchivedFileSet arg0 )
            throws ArchiverException
        {
        }

        public void addArchivedFileSet( File arg0, String arg1 )
            throws ArchiverException
        {
        }

        public void addArchivedFileSet( File arg0, String[] arg1, String[] arg2 )
            throws ArchiverException
        {
        }

        public void addArchivedFileSet( File arg0, String arg1, String[] arg2, String[] arg3 )
            throws ArchiverException
        {
        }

        public void addDirectory( File arg0 )
            throws ArchiverException
        {
        }

        public void addDirectory( File arg0, String arg1 )
            throws ArchiverException
        {
        }

        public void addDirectory( File arg0, String[] arg1, String[] arg2 )
            throws ArchiverException
        {
        }

        public void addDirectory( File arg0, String arg1, String[] arg2, String[] arg3 )
            throws ArchiverException
        {
        }

        public void addFile( File arg0, String arg1 )
            throws ArchiverException
        {
        }

        public void addFile( File arg0, String arg1, int arg2 )
            throws ArchiverException
        {
        }

        public void addFileSet( FileSet arg0 )
            throws ArchiverException
        {
        }

        public void addResource( PlexusIoResource arg0, String arg1, int arg2 )
            throws ArchiverException
        {
        }

        public void addResources( PlexusIoResourceCollection arg0 )
            throws ArchiverException
        {
        }

        public void createArchive()
            throws ArchiverException, IOException
        {
        }

        public int getDefaultDirectoryMode()
        {
            return 0;
        }

        public int getDefaultFileMode()
        {
            return 0;
        }

        public File getDestFile()
        {
            return null;
        }

        public Map getFiles()
        {
            return null;
        }

        public boolean getIncludeEmptyDirs()
        {
            return false;
        }

        public ResourceIterator getResources()
            throws ArchiverException
        {
            return null;
        }

        public boolean isForced()
        {
            return false;
        }

        public boolean isSupportingForced()
        {
            return false;
        }

        public void setDefaultDirectoryMode( int arg0 )
        {
        }

        public void setDefaultFileMode( int arg0 )
        {
        }

        public void setDestFile( File arg0 )
        {
        }

        public void setDotFileDirectory( File arg0 )
        {
        }

        public void setForced( boolean arg0 )
        {
        }

        public void setIncludeEmptyDirs( boolean arg0 )
        {
        }

        public String getDuplicateBehavior()
        {
            return Archiver.DUPLICATES_ADD;
        }

        public void setDuplicateBehavior( String duplicate )
        {
        }

        public int getDirectoryMode()
        {
            return 0;
        }

        public int getFileMode()
        {
            return 0;
        }

        public int getOverrideDirectoryMode()
        {
            return 0;
        }

        public int getOverrideFileMode()
        {
            return 0;
        }

        public void setDirectoryMode( int mode )
        {
        }

        public void setFileMode( int mode )
        {
        }

    }

}
