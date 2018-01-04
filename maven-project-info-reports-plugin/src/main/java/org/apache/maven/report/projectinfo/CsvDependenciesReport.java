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

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.report.projectinfo.dependencies.Dependencies;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.apache.maven.report.projectinfo.dependencies.renderer.CSVDependenciesRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.jar.classes.JarClassesAnalysis;

/**
 * Generate simple csv dependecy file report
 */
@Mojo( name = "dependenciescsv", requiresDependencyResolution = ResolutionScope.TEST )
public class CsvDependenciesReport extends AbstractProjectInfoReport
{

    private static final String SKIN_TEMPLATE_LOCATION = "META-INF/maven/csv.vm";
    
    @Component
    private WagonManager wagonManager;
    
    @Component( hint = "default" )
    private DependencyGraphBuilder dependencyGraphBuilder;
    
    @Component
    private JarClassesAnalysis classesAnalyzer;

    @Component
    private RepositoryMetadataManager repositoryMetadataManager;

    @Component
    private ArtifactFactory artifactFactory;
    
    
    @Override
    public String getOutputName() 
    {
        return "dependencies";
    }

    @Override
    protected String getI18Nsection()
    {
        return "dependencies";
    }
    
    @Override
    protected String getExt()
    {
        return ".csv";
    }
    
    @Override
    protected SiteRenderingContext getSiteRenderingContext( DecorationModel model, Map<String, Object> attributes, 
                                    Locale locale, Artifact defaultSkin ) throws IOException 
    {
        SiteRenderingContext context = new SiteRenderingContext();
        context.setTemplateName( SKIN_TEMPLATE_LOCATION );
        context.setTemplateProperties( attributes );
        context.setLocale( locale );
        context.setDecoration( model );
        return context;
    }
    
    @Override
    protected void executeReport( Locale locale ) throws MavenReportException
    {
        @SuppressWarnings( "unchecked" )
        RepositoryUtils repoUtils = new RepositoryUtils( getLog(), wagonManager, settings, mavenProjectBuilder,
                                        factory, resolver,
                                        project.getRemoteArtifactRepositories(),
                                        project.getPluginArtifactRepositories(), localRepository,
                                        repositoryMetadataManager );

        DependencyNode dependencyNode = resolveProject();

        Dependencies dependencies = new Dependencies( project, dependencyNode, classesAnalyzer );

        CSVDependenciesRenderer r = new CSVDependenciesRenderer( getSink(), getI18N( locale ),
                                        locale, dependencies, getLog(),
                                        repoUtils );
        
        r.render();
    }
    
    private DependencyNode resolveProject()
    {
        try
        {
            ArtifactFilter artifactFilter = new ScopeArtifactFilter( Artifact.SCOPE_TEST );
            return dependencyGraphBuilder.buildDependencyGraph( project, artifactFilter );
        }
        catch ( DependencyGraphBuilderException e )
        {
            getLog().error( "Unable to build dependency tree.", e );
            return null;
        }
    }
    
}
