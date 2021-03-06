package org.apache.maven.report.projectinfo;

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

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Model;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Generates the Project Team report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @since 2.0
 * @goal project-team
 */
public class TeamListReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void executeReport( Locale locale )
    {
        TeamListRenderer r = new TeamListRenderer( getSink(), project.getModel(), i18n, locale );

        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "team-list";
    }

    protected String getI18Nsection()
    {
        return "team-list";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private static class TeamListRenderer
        extends AbstractProjectInfoRenderer
    {
        private static final String PROPERTIES = "properties";

        private static final String TIME_ZONE = "timeZone";

        private static final String ROLES = "roles";

        private static final String ORGANIZATION_URL = "organizationUrl";

        private static final String ORGANIZATION = "organization";

        private static final String URL = "url";

        private static final String EMAIL = "email";

        private static final String NAME = "name";

        private static final String ID = "id";

        private Model model;

        private static final String[] EMPTY_STRING_ARRAY = new String[0];

        TeamListRenderer( Sink sink, Model model, I18N i18n, Locale locale )
        {
            super( sink, i18n, locale );

            this.model = model;
        }

        protected String getI18Nsection()
        {
            return "team-list";
        }

        /** {@inheritDoc} */
        public void renderBody()
        {
            startSection( getI18nString( "intro.title" ) );

            // To handle JS
            StringBuffer javascript = new StringBuffer( "function offsetDate(id, offset) {" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( "    var now = new Date();" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( "    var nowTime = now.getTime();" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( "    var localOffset = now.getTimezoneOffset();" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( "    var developerTime = nowTime + ( offset * 60 * 60 * 1000 )"
                               + "+ ( localOffset * 60 * 1000 );" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( "    var developerDate = new Date(developerTime);" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( SystemUtils.LINE_SEPARATOR );
            javascript.append( "    document.getElementById(id).innerHTML = developerDate;" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( "}" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( SystemUtils.LINE_SEPARATOR);
            javascript.append( "function init(){" ).append( SystemUtils.LINE_SEPARATOR );

            // Introduction
            paragraph( getI18nString( "intro.description1" ) );
            paragraph( getI18nString( "intro.description2" ) );

            // Developer section
            List<Developer> developers = model.getDevelopers();

            startSection( getI18nString( "developers.title" ) );

            if ( developers == null || developers.isEmpty() )
            {
                paragraph( getI18nString( "nodeveloper" ) );
            }
            else
            {
                paragraph( getI18nString( "developers.intro" ) );

                startTable();

                // By default we think that all headers not required
                Map<String, Boolean> headersMap = new HashMap<String, Boolean>();
                // set true for headers that are required
                checkRequiredHeaders( headersMap, developers );
                String[] requiredHeaders = getRequiredDevHeaderArray( headersMap );

                tableHeader( requiredHeaders );

                // To handle JS
                int developersRows = 0;
                for ( Developer developer : developers )
                {
                    renderDeveloper( developer, developersRows, headersMap, javascript );

                    developersRows++;
                }

                endTable();
            }

            endSection();

            // contributors section
            List<Contributor> contributors = model.getContributors();

            startSection( getI18nString( "contributors.title" ) );

            if ( contributors == null || contributors.isEmpty() )
            {
                paragraph( getI18nString( "nocontributor" ) );
            }
            else
            {
                paragraph( getI18nString( "contributors.intro" ) );

                startTable();

                Map<String, Boolean> headersMap = new HashMap<String, Boolean>();
                checkRequiredHeaders( headersMap, contributors );
                String[] requiredHeaders = getRequiredContrHeaderArray( headersMap );

                tableHeader( requiredHeaders );

                // To handle JS
                int contributorsRows = 0;
                for ( Contributor contributor : contributors )
                {
                    renderContributor( contributor, contributorsRows, headersMap, javascript );

                    contributorsRows++;
                }

                endTable();
            }

            // To handle JS
            javascript.append( "}" ).append( SystemUtils.LINE_SEPARATOR ).append( SystemUtils.LINE_SEPARATOR )
                .append( "window.onLoad = init();" ).append( SystemUtils.LINE_SEPARATOR );
            javaScript( javascript.toString() );

            endSection();

            endSection();
        }

        private void renderDeveloper( Developer developer, int developerRow, Map<String, Boolean> headersMap,
                                      StringBuffer javascript )
        {
            // To handle JS
            sink.tableRow();

            if ( headersMap.get( ID ) == Boolean.TRUE )
            {
                tableCell( "<a name=\"" + developer.getId() + "\"></a>" + developer.getId(), true );
            }
            if ( headersMap.get( NAME ) == Boolean.TRUE )
            {
                tableCell( developer.getName() );
            }
            if ( headersMap.get( EMAIL ) == Boolean.TRUE )
            {
                tableCell( createLinkPatternedText( developer.getEmail(), developer.getEmail() ) );
            }
            if ( headersMap.get( URL ) == Boolean.TRUE )
            {
                tableCellForUrl( developer.getUrl() );
            }
            if ( headersMap.get( ORGANIZATION ) == Boolean.TRUE )
            {
                tableCell( developer.getOrganization() );
            }
            if ( headersMap.get( ORGANIZATION_URL ) == Boolean.TRUE )
            {
                tableCellForUrl( developer.getOrganizationUrl() );
            }
            if ( headersMap.get( ROLES ) == Boolean.TRUE )
            {
                if ( developer.getRoles() != null )
                {
                    // Comma separated roles
                    tableCell( StringUtils.join( developer.getRoles().toArray( EMPTY_STRING_ARRAY ), ", " ) );
                }
                else
                {
                    tableCell( null );
                }
            }
            if ( headersMap.get( TIME_ZONE ) == Boolean.TRUE )
            {
                tableCell( developer.getTimezone() );

                // To handle JS
                sink.tableCell();
                sink.rawText( "<span id=\"developer-" + developerRow + "\">" );
                text( developer.getTimezone() );
                if ( !StringUtils.isEmpty( developer.getTimezone() ) )
                {
                    javascript.append( "    offsetDate('developer-" ).append( developerRow ).append( "', '" );
                    javascript.append( developer.getTimezone() );
                    javascript.append( "');" ).append( SystemUtils.LINE_SEPARATOR );
                }
                sink.rawText( "</span>" );
                sink.tableCell_();
            }

            if ( headersMap.get( PROPERTIES ) == Boolean.TRUE )
            {
                Properties props = developer.getProperties();
                if ( props != null )
                {
                    tableCell( propertiesToString( props ) );
                }
                else
                {
                    tableCell( null );
                }
            }

            sink.tableRow_();
        }

        private void renderContributor( Contributor contributor, int contributorRow, Map<String, Boolean> headersMap,
                                        StringBuffer javascript )
        {
            sink.tableRow();
            if ( headersMap.get( NAME ) == Boolean.TRUE )
            {
                tableCell( contributor.getName() );
            }
            if ( headersMap.get( EMAIL ) == Boolean.TRUE )
            {
                tableCell( createLinkPatternedText( contributor.getEmail(), contributor.getEmail() ) );
            }
            if ( headersMap.get( URL ) == Boolean.TRUE )
            {
                tableCellForUrl( contributor.getUrl() );
            }
            if ( headersMap.get( ORGANIZATION ) == Boolean.TRUE )
            {
                tableCell( contributor.getOrganization() );
            }
            if ( headersMap.get( ORGANIZATION_URL ) == Boolean.TRUE )
            {
                tableCellForUrl( contributor.getOrganizationUrl() );
            }
            if ( headersMap.get( ROLES ) == Boolean.TRUE )
            {
                if ( contributor.getRoles() != null )
                {
                    // Comma separated roles
                    tableCell( StringUtils.join( contributor.getRoles().toArray( EMPTY_STRING_ARRAY ), ", " ) );
                }
                else
                {
                    tableCell( null );
                }

            }
            if ( headersMap.get( TIME_ZONE ) == Boolean.TRUE )
            {
                tableCell( contributor.getTimezone() );

                // To handle JS
                sink.tableCell();
                sink.rawText( "<span id=\"contributor-" + contributorRow + "\">" );
                text( contributor.getTimezone() );
                if ( !StringUtils.isEmpty( contributor.getTimezone() ) )
                {
                    javascript.append( "    offsetDate('contributor-" ).append( contributorRow )
                        .append( "', '" );
                    javascript.append( contributor.getTimezone() );
                    javascript.append( "');" ).append( SystemUtils.LINE_SEPARATOR );
                }
                sink.rawText( "</span>" );
                sink.tableCell_();
            }

            if ( headersMap.get( PROPERTIES ) == Boolean.TRUE )
            {
                Properties props = contributor.getProperties();
                if ( props != null )
                {
                    tableCell( propertiesToString( props ) );
                }
                else
                {
                    tableCell( null );
                }
            }

            sink.tableRow_();
        }

        /**
         * @param requiredHeaders
         * @return
         */
        private String[] getRequiredContrHeaderArray( Map<String, Boolean> requiredHeaders )
        {
            List<String> requiredArray = new ArrayList<String>();
            String name = getI18nString( "contributors.name" );
            String email = getI18nString( "contributors.email" );
            String url = getI18nString( "contributors.url" );
            String organization = getI18nString( "contributors.organization" );
            String organizationUrl = getI18nString( "contributors.organizationurl" );
            String roles = getI18nString( "contributors.roles" );
            String timeZone = getI18nString( "contributors.timezone" );
            String actualTime = getI18nString( "contributors.actualtime" );
            String properties = getI18nString( "contributors.properties" );

            setRequiredArray( requiredHeaders, requiredArray, name, email, url, organization, organizationUrl, roles,
                              timeZone, actualTime, properties );

            String[] array = new String[requiredArray.size()];
            for ( int i = 0; i < array.length; i++ )
            {
                array[i] = (String) requiredArray.get( i );
            }

            return array;
        }

        /**
         * @param requiredHeaders
         * @return
         */
        private String[] getRequiredDevHeaderArray( Map<String, Boolean> requiredHeaders )
        {
            List<String> requiredArray = new ArrayList<String>();

            String id = getI18nString( "developers.id" );
            String name = getI18nString( "developers.name" );
            String email = getI18nString( "developers.email" );
            String url = getI18nString( "developers.url" );
            String organization = getI18nString( "developers.organization" );
            String organizationUrl = getI18nString( "developers.organizationurl" );
            String roles = getI18nString( "developers.roles" );
            String timeZone = getI18nString( "developers.timezone" );
            String actualTime = getI18nString( "developers.actualtime" );
            String properties = getI18nString( "developers.properties" );

            if ( requiredHeaders.get( ID ) == Boolean.TRUE )
            {
                requiredArray.add( id );
            }

            setRequiredArray( requiredHeaders, requiredArray, name, email, url, organization, organizationUrl, roles,
                              timeZone, actualTime, properties );

            String[] array = new String[requiredArray.size()];
            for ( int i = 0; i < array.length; i++ )
            {
                array[i] = (String) requiredArray.get( i );
            }

            return array;
        }

        /**
         * @param requiredHeaders
         * @param requiredArray
         * @param name
         * @param email
         * @param url
         * @param organization
         * @param organizationUrl
         * @param roles
         * @param timeZone
         * @param actualTime
         * @param properties
         */
        private void setRequiredArray( Map<String, Boolean> requiredHeaders, List<String> requiredArray, String name,
                                       String email, String url, String organization, String organizationUrl,
                                       String roles, String timeZone, String actualTime, String properties )
        {
            if ( requiredHeaders.get( NAME ) == Boolean.TRUE )
            {
                requiredArray.add( name );
            }
            if ( requiredHeaders.get( EMAIL ) == Boolean.TRUE )
            {
                requiredArray.add( email );
            }
            if ( requiredHeaders.get( URL ) == Boolean.TRUE )
            {
                requiredArray.add( url );
            }
            if ( requiredHeaders.get( ORGANIZATION ) == Boolean.TRUE )
            {
                requiredArray.add( organization );
            }
            if ( requiredHeaders.get( ORGANIZATION_URL ) == Boolean.TRUE )
            {
                requiredArray.add( organizationUrl );
            }
            if ( requiredHeaders.get( ROLES ) == Boolean.TRUE )
            {
                requiredArray.add( roles );
            }
            if ( requiredHeaders.get( TIME_ZONE ) == Boolean.TRUE )
            {
                requiredArray.add( timeZone );
                requiredArray.add( actualTime );
            }

            if ( requiredHeaders.get( PROPERTIES ) == Boolean.TRUE )
            {
                requiredArray.add( properties );
            }
        }

        /**
         * @param requiredHeaders
         * @param units
         */
        private void checkRequiredHeaders( Map<String, Boolean> requiredHeaders, List<?> units )
        {
            requiredHeaders.put( ID, Boolean.FALSE );
            requiredHeaders.put( NAME, Boolean.FALSE );
            requiredHeaders.put( EMAIL, Boolean.FALSE );
            requiredHeaders.put( URL, Boolean.FALSE );
            requiredHeaders.put( ORGANIZATION, Boolean.FALSE );
            requiredHeaders.put( ORGANIZATION_URL, Boolean.FALSE );
            requiredHeaders.put( ROLES, Boolean.FALSE );
            requiredHeaders.put( TIME_ZONE, Boolean.FALSE );
            requiredHeaders.put( PROPERTIES, Boolean.FALSE );

            for ( Object unit : units )
            {
                String name = null;
                String email = null;
                String url = null;
                String organization = null;
                String organizationUrl = null;
                List<String> roles = null;
                String timeZone = null;
                Properties properties = null;

                if ( unit.getClass().getName().equals( Contributor.class.getName() ) )
                {
                    Contributor contributor = (Contributor) unit;
                    name = contributor.getName();
                    email = contributor.getEmail();
                    url = contributor.getUrl();
                    organization = contributor.getOrganization();
                    organizationUrl = contributor.getOrganizationUrl();
                    roles = contributor.getRoles();
                    timeZone = contributor.getTimezone();
                    properties = contributor.getProperties();
                }
                else
                {
                    Developer developer = (Developer) unit;
                    name = developer.getName();
                    email = developer.getEmail();
                    url = developer.getUrl();
                    organization = developer.getOrganization();
                    organizationUrl = developer.getOrganizationUrl();
                    roles = developer.getRoles();
                    timeZone = developer.getTimezone();
                    properties = developer.getProperties();
                    if ( StringUtils.isNotEmpty( developer.getId() ) )
                    {
                        requiredHeaders.put( ID, Boolean.TRUE );
                    }
                }
                if ( StringUtils.isNotEmpty( name ) )
                {
                    requiredHeaders.put( NAME, Boolean.TRUE );
                }
                if ( StringUtils.isNotEmpty( email ) )
                {
                    requiredHeaders.put( EMAIL, Boolean.TRUE );
                }
                if ( StringUtils.isNotEmpty( url ) )
                {
                    requiredHeaders.put( URL, Boolean.TRUE );
                }
                if ( StringUtils.isNotEmpty( organization ) )
                {
                    requiredHeaders.put( ORGANIZATION, Boolean.TRUE );
                }
                if ( StringUtils.isNotEmpty( organizationUrl ) )
                {
                    requiredHeaders.put( ORGANIZATION_URL, Boolean.TRUE );
                }
                if ( null != roles && !roles.isEmpty() )
                {
                    requiredHeaders.put( ROLES, Boolean.TRUE );
                }
                if ( StringUtils.isNotEmpty( timeZone ) )
                {
                    requiredHeaders.put( TIME_ZONE, Boolean.TRUE );
                }
                if ( null != properties && !properties.isEmpty() )
                {
                    requiredHeaders.put( PROPERTIES, Boolean.TRUE );
                }
            }
        }

        /**
         * Create a table cell with a link to the given url. The url is not validated.
         *
         * @param url
         */
        private void tableCellForUrl( String url )
        {
            sink.tableCell();

            if ( StringUtils.isEmpty( url ) )
            {
                text( url );
            }
            else
            {
                link( url, url );
            }

            sink.tableCell_();
        }
    }
}
