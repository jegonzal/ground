package edu.berkeley.ground.resources;

import com.codahale.metrics.annotation.Timed;
import edu.berkeley.ground.api.usage.LineageEdge;
import edu.berkeley.ground.api.usage.LineageEdgeFactory;
import edu.berkeley.ground.api.usage.LineageEdgeVersion;
import edu.berkeley.ground.api.usage.LineageEdgeVersionFactory;
import edu.berkeley.ground.exceptions.GroundException;
import io.dropwizard.jersey.params.NonEmptyStringParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/lineage")
@Produces(MediaType.APPLICATION_JSON)
public class LineageEdgesResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(LineageEdgesResource.class);

    private LineageEdgeFactory lineageEdgeFactory;
    private LineageEdgeVersionFactory lineageEdgeVersionFactory;

    public LineageEdgesResource(LineageEdgeFactory lineageEdgeFactory, LineageEdgeVersionFactory lineageEdgeVersionFactory) {
        this.lineageEdgeFactory = lineageEdgeFactory;
        this.lineageEdgeVersionFactory = lineageEdgeVersionFactory;
    }

    @GET
    @Timed
    @Path("/{name}")
    public LineageEdge getLineageEdge(@PathParam("name") String name) throws GroundException {
        LOGGER.info("Retrieving lineage edge " + name + ".");
        return this.lineageEdgeFactory.retrieveFromDatabase(name);
    }

    @GET
    @Timed
    @Path("/versions/{id}")
    public LineageEdgeVersion getLineageEdgeVersion(@PathParam("id") String id) throws GroundException {
        LOGGER.info("Retrieving lineage edge version " + id + ".");
        return this.lineageEdgeVersionFactory.retrieveFromDatabase(id);
    }

    @POST
    @Timed
    @Path("/{name}")
    public LineageEdge createLineageEdge(@PathParam("name") String name) throws GroundException {
        LOGGER.info("Creating lineage edge " + name + ".");
        return this.lineageEdgeFactory.create(name);
    }

    @POST
    @Timed
    @Path("/versions")
    public LineageEdgeVersion createLineageEdgeVersion(@Valid LineageEdgeVersion lineageEdgeVersion, @QueryParam("parent") NonEmptyStringParam parentId) throws GroundException {
        LOGGER.info("Creating lineage edge version in lineage edge " + lineageEdgeVersion.getLineageEdgeId() + ".");
        return this.lineageEdgeVersionFactory.create(lineageEdgeVersion.getTags(),
                                                     lineageEdgeVersion.getStructureVersionId(),
                                                     lineageEdgeVersion.getReference(),
                                                     lineageEdgeVersion.getParameters(),
                                                     lineageEdgeVersion.getFromId(),
                                                     lineageEdgeVersion.getToId(),
                                                     lineageEdgeVersion.getLineageEdgeId(),
                                                     parentId.get());
    }
}
