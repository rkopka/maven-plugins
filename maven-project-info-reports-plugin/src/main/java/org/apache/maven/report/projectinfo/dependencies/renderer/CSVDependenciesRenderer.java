package org.apache.maven.report.projectinfo.dependencies.renderer;

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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.License;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.report.projectinfo.AbstractProjectInfoRenderer;
import org.apache.maven.report.projectinfo.dependencies.Dependencies;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.codehaus.plexus.i18n.I18N;

/**
 * @author Rafał Kopka
 *
 */
public class CSVDependenciesRenderer extends AbstractProjectInfoRenderer
{

    private final Dependencies dependencies;
    
    private final Log log;
    
    private final RepositoryUtils repoUtils;
    
    public CSVDependenciesRenderer( Sink sink, I18N i18n, Locale locale, Dependencies dependencies,
                                    Log log, RepositoryUtils repoUtils )
    {
        super( sink, i18n, locale );
        this.dependencies = dependencies; 
        this.log = log;
        this.repoUtils = repoUtils;
    }

    @Override
    protected String getI18Nsection()
    {
        return "dependencies";
    }
    
    @Override
    public void render()
    {
        renderBody();
        sink.flush();
        sink.close();
    }

    @Override
    protected void renderBody() 
    {
        try
        {
            List<Artifact> alldeps = dependencies.getAllDependencies();
            alldeps.sort( new Comparator<Artifact>() 
            {
                @Override
                public int compare( Artifact o1, Artifact o2 )
                {
                    return o1.compareTo( o2 );
                }
            }
            );
            resolveAtrifacts( alldeps );
            
            StringBuilder sb = new StringBuilder();
            String version = getI18nString( "column.version" );
            String desc = getI18nString( "column.description" );
            String licensesBundle = getI18nString( "column.licenses" );
            String fileName = getI18nString( "file.details.column.file" );
            String name = getI18nString( "name" );
            
            
            sb.append( name );
            sb.append( ";" );
            sb.append( version );
            sb.append( ";" );
            sb.append( fileName );
            sb.append( ";" );
            sb.append( licensesBundle );
            sb.append( ";" );
            sb.append( desc );
            sb.append( ";" );
            sb.append( "\n" );
            sink.rawText( sb.toString() );
            for ( Artifact artifact : alldeps )
            {
                if ( artifact.getFile() == null )
                {
                    log.warn( "Artifact " + artifact.getId() + " has no file"
                        + " and won't be listed in dependency files details." );
                    continue;
                }
                try 
                {
                    sb = new StringBuilder();
                    MavenProject artifactProject = repoUtils.getMavenProjectFromRepository( artifact );
                    String artifactName = artifactProject.getName();
                    String artifactDescription = artifactProject.getDescription();
                    sb.append( artifactName.replaceAll( "\"", "" ) );
                    sb.append( ";" );
                    
                    sb.append( artifact.getVersion() );
                    sb.append( ";" );
                    
                    
                    sb.append( artifact.getFile().getName() );
                    sb.append( ";" );
                    
                    
                    @SuppressWarnings( "unchecked" )
                    List<License> licenses = artifactProject.getLicenses();
                    
                    if ( !licenses.isEmpty() )
                    {
                        List<String> licences = new ArrayList<String>();
                        for ( License license : licenses )
                        {
                            if ( !StringUtils.isEmpty( license.getName() ) )
                            {
                                licences.add( license.getName() );
                            }
                        }
                        sb.append( String.join( ",", licences ) );
                    }
                    else
                    {
                        sb.append( getI18nString( "licenses", "nolicense" ) );
                    }
                    sb.append( ";" );
                    if ( !StringUtils.isEmpty( artifactDescription ) ) 
                    {
                        sb.append( artifactDescription.replaceAll( "\n", "" ) );
                    }
                    sb.append( ";" );
                    sb.append( "\n" );
                    sink.rawText( sb.toString() );
                } 
                catch ( ProjectBuildingException e ) 
                {
                    e.printStackTrace();
                }
                
                
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        
    }
    
    private void resolveAtrifacts( List<Artifact> artifacts )
    {
        for ( Artifact artifact : artifacts )
        {
            if ( artifact.getFile() == null )
            {
                if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
                {
                    continue;
                }

                try
                {
                    repoUtils.resolve( artifact );
                }
                catch ( ArtifactResolutionException e )
                {
                    log.error( "Artifact " + artifact.getId() + " can't be resolved.", e );
                    continue;
                }
                catch ( ArtifactNotFoundException e )
                {
                    if ( ( dependencies.getProject().getGroupId().equals( artifact.getGroupId() ) )
                        && ( dependencies.getProject().getArtifactId().equals( artifact.getArtifactId() ) )
                        && ( dependencies.getProject().getVersion().equals( artifact.getVersion() ) ) )
                    {
                        log.warn( "The artifact of this project has never been deployed." );
                    }
                    else
                    {
                        log.error( "Artifact " + artifact.getId() + " not found.", e );
                    }

                    continue;
                }

                if ( artifact.getFile() == null )
                {
                    log.error( "Artifact " + artifact.getId() + " has no file, even after resolution." );
                }
            }
        }
    }

}
