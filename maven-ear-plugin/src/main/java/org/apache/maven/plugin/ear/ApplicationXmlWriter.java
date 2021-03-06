package org.apache.maven.plugin.ear;

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

import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.Writer;
import java.util.Iterator;

/**
 * An <tt>XmlWriter</tt> based implementation used to generate an
 * <tt>application.xml</tt> file
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
final class ApplicationXmlWriter
    extends AbstractXmlWriter
{
    public static final String DOCTYPE_1_3 =
        "application PUBLIC\n" + "\t\"-//Sun Microsystems, Inc.//DTD J2EE Application 1.3//EN\"\n" +
            "\t\"http://java.sun.com/dtd/application_1_3.dtd\"";

    private static final String APPLICATION_ELEMENT = "application";


    private final String version;

    ApplicationXmlWriter( String version, String encoding )
    {
        super( encoding );
        this.version = version;
    }

    public void write( ApplicationXmlWriterContext context )
        throws EarPluginException
    {
        Writer w = initializeWriter( context.getDestinationFile() );

        XMLWriter writer = null;
        if ( GenerateApplicationXmlMojo.VERSION_1_3.equals( version ) )
        {
            writer = initializeRootElementOneDotThree( w );
        }
        else if ( GenerateApplicationXmlMojo.VERSION_1_4.equals( version ) )
        {
            writer = initializeRootElementOneDotFour( w );
        }
        else if ( GenerateApplicationXmlMojo.VERSION_5.equals( version ) )
        {
            writer = initializeRootElementFive( w );
        }
        else if ( GenerateApplicationXmlMojo.VERSION_6.equals( version ) )
        {
            writer = initializeRootElementSix( w );
        }

        // IMPORTANT: the order of the description and display-name elements was
        // reversed between J2EE 1.3 and J2EE 1.4.
        if ( GenerateApplicationXmlMojo.VERSION_1_3.equals( version ) )
        {
            writeDisplayName( context.getDisplayName(), writer );
            writeDescription( context.getDescription(), writer );
        }
        else
        {
            writeDescription( context.getDescription(), writer );
            writeDisplayName( context.getDisplayName(), writer );
        }
        // Do not change this unless you really know what you're doing :)


        final Iterator moduleIt = context.getEarModules().iterator();
        while ( moduleIt.hasNext() )
        {
            EarModule module = (EarModule) moduleIt.next();
            module.appendModule( writer, version );
        }

        final Iterator securityRoleIt = context.getSecurityRoles().iterator();
        while ( securityRoleIt.hasNext() )
        {
            SecurityRole securityRole = (SecurityRole) securityRoleIt.next();
            securityRole.appendSecurityRole( writer );
        }

        if ( GenerateApplicationXmlMojo.VERSION_5.equals( version ) )
        {
            writeLibraryDirectory( context.getLibraryDirectory(), writer );
        }

        writer.endElement();

        close( w );
    }

    private void writeDescription( String description, XMLWriter writer )
    {
        if ( description != null )
        {
            writer.startElement( "description" );
            writer.writeText( description );
            writer.endElement();
        }
    }

    private void writeDisplayName( String displayName, XMLWriter writer )
    {
        if ( displayName != null )
        {
            writer.startElement( "display-name" );
            writer.writeText( displayName );
            writer.endElement();
        }
    }

    private void writeLibraryDirectory( String libraryDirectory, XMLWriter writer )
    {
        if ( libraryDirectory != null )
        {
            writer.startElement( "library-directory" );
            writer.writeText( libraryDirectory );
            writer.endElement();
        }
    }

    private XMLWriter initializeRootElementOneDotThree( Writer w )
    {
        XMLWriter writer = initializeXmlWriter( w, DOCTYPE_1_3 );
        writer.startElement( APPLICATION_ELEMENT );
        return writer;
    }

    private XMLWriter initializeRootElementOneDotFour( Writer w )
    {
        XMLWriter writer = initializeXmlWriter( w, null );
        writer.startElement( APPLICATION_ELEMENT );
        writer.addAttribute( "xmlns", "http://java.sun.com/xml/ns/j2ee" );
        writer.addAttribute( "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance" );
        writer.addAttribute( "xsi:schemaLocation",
                             "http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/application_1_4.xsd" );
        writer.addAttribute( "version", "1.4" );
        return writer;
    }

    private XMLWriter initializeRootElementFive( Writer w )
    {
        XMLWriter writer = initializeXmlWriter( w, null );
        writer.startElement( APPLICATION_ELEMENT );
        writer.addAttribute( "xmlns", "http://java.sun.com/xml/ns/javaee" );
        writer.addAttribute( "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance" );
        writer.addAttribute( "xsi:schemaLocation",
                             "http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/application_5.xsd" );
        writer.addAttribute( "version", "5" );
        return writer;
    }

    private XMLWriter initializeRootElementSix( Writer w )
    {
        XMLWriter writer = initializeXmlWriter( w, null );
        writer.startElement( APPLICATION_ELEMENT );
        writer.addAttribute( "xmlns", "http://java.sun.com/xml/ns/javaee" );
        writer.addAttribute( "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance" );
        writer.addAttribute( "xsi:schemaLocation",
                             "http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/application_6.xsd" );
        writer.addAttribute( "version", "6" );
        return writer;
    }

}
