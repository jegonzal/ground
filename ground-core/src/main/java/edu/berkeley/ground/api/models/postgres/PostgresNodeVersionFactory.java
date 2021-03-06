package edu.berkeley.ground.api.models.postgres;

import edu.berkeley.ground.api.models.*;
import edu.berkeley.ground.api.versions.Type;
import edu.berkeley.ground.db.DBClient;
import edu.berkeley.ground.db.DbDataContainer;
import edu.berkeley.ground.db.PostgresClient;
import edu.berkeley.ground.db.PostgresClient.PostgresConnection;
import edu.berkeley.ground.db.QueryResults;
import edu.berkeley.ground.exceptions.GroundException;
import edu.berkeley.ground.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PostgresNodeVersionFactory extends NodeVersionFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresNodeVersionFactory.class);
    private PostgresClient dbClient;

    private PostgresNodeFactory nodeFactory;
    private PostgresRichVersionFactory richVersionFactory;

    public PostgresNodeVersionFactory(PostgresNodeFactory nodeFactory, PostgresRichVersionFactory richVersionFactory, PostgresClient dbClient) {
        this.dbClient = dbClient;
        this.nodeFactory = nodeFactory;
        this.richVersionFactory = richVersionFactory;
    }


    public NodeVersion create(Optional<Map<String, Tag>> tags,
                              Optional<String> structureVersionId,
                              Optional<String> reference,
                              Optional<Map<String, String>> parameters,
                              String nodeId,
                              Optional<String> parentId) throws GroundException {

        PostgresConnection connection = this.dbClient.getConnection();

        try {
            String id = IdGenerator.generateId(nodeId);

            // add the id of the version to the tag
            tags = tags.map(tagsMap ->
                                    tagsMap.values().stream().collect(Collectors.toMap(Tag::getKey, tag -> new Tag(id, tag.getKey(), tag.getValue(), tag.getValueType())))
            );

            this.richVersionFactory.insertIntoDatabase(connection, id, tags, structureVersionId, reference, parameters);

            List<DbDataContainer> insertions = new ArrayList<>();
            insertions.add(new DbDataContainer("id", Type.STRING, id));
            insertions.add(new DbDataContainer("node_id", Type.STRING, nodeId));

            connection.insert("NodeVersions", insertions);

            this.nodeFactory.update(connection, nodeId, id, parentId);

            connection.commit();
            LOGGER.info("Created node version " + id + " in node " + nodeId + ".");

            return NodeVersionFactory.construct(id, tags, structureVersionId, reference, parameters, nodeId);
        } catch (GroundException e) {
            connection.abort();

            throw e;
        }
    }

    public NodeVersion retrieveFromDatabase(String id) throws GroundException {
        PostgresConnection connection = this.dbClient.getConnection();

        try {
            RichVersion version = this.richVersionFactory.retrieveFromDatabase(connection, id);

            List<DbDataContainer> predicates = new ArrayList<>();
            predicates.add(new DbDataContainer("id", Type.STRING, id));

            QueryResults resultSet = connection.equalitySelect("NodeVersions", DBClient.SELECT_STAR, predicates);
            String nodeId = resultSet.getString(2);

            connection.commit();
            LOGGER.info("Retrieved node version " + id + " in node " + nodeId + ".");

            return NodeVersionFactory.construct(id, version.getTags(), version.getStructureVersionId(), version.getReference(), version.getParameters(), nodeId);
        } catch (GroundException e) {
            connection.abort();

            throw e;
        }
    }

    public List<String> getTransitiveClosure(String nodeVersionId) throws GroundException {
        PostgresConnection connection = this.dbClient.getConnection();
        List<String> result = connection.transitiveClosure(nodeVersionId);

        connection.commit();
        return result;
    }
}
