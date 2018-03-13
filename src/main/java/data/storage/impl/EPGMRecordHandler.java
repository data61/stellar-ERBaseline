package data.storage.impl;

import data.Attribute;
import data.Record;
import data.storage.DataSource;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.lang.reflect.Type;
import javafx.util.Pair;

import org.apache.spark.sql.SparkSession;
import sh.serene.stellarutils.entities.*;
import sh.serene.stellarutils.graph.api.StellarBackEndFactory;
import sh.serene.stellarutils.graph.api.StellarGraph;
import sh.serene.stellarutils.graph.api.StellarGraphCollection;
import sh.serene.stellarutils.graph.impl.local.*;
import sh.serene.stellarutils.graph.impl.spark.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


public class EPGMRecordHandler implements DataSource {

    private  StellarBackEndFactory stellarFactory;
    private Map<String, Set<String>> attributesMap;
    private Set<Record> records;
    private StellarGraphCollection graphCollection;
    private Gson gson;

    public EPGMRecordHandler(SparkSession sparkSession) {
        if (sparkSession != null) stellarFactory = new SparkBackEndFactory(sparkSession);
        else stellarFactory = new LocalBackEndFactory();
        attributesMap = new HashMap<>();
        records = new HashSet<>();
        gson = new Gson();
    }

    @Override
    public Set<Record> getAllRecords() {
        return records;
    }

    @Override
    public Iterator iterator() {
        return records.iterator();
    }

    public void recordsFromEPGM(String dir)
            throws IOException {

        System.out.println("Parsing EPGM: " + dir);
        graphCollection =
                stellarFactory.reader()
                        .format("json").getGraphCollection(dir);

        StellarGraph graph = graphCollection.get(0);
        List<Vertex> vertexes = graph.getVertices().asList();
        List<Edge> edges = graph.getEdges().asList();

        Set<String> labels = vertexes.stream().map(vertex -> vertex.getLabel()).collect(Collectors.toSet());
        String rootSubgraph = labels.iterator().next();

        if (labels.size() > 1) {
            HashMap<String, Integer> rootMap = new HashMap<>();
            labels.forEach(label -> rootMap.put(label, 0));

            edges.forEach(e -> {
                        vertexes.stream().filter(v -> e.getSrc().equals(v.getId()))
                                .forEach(v -> rootMap.put(v.getLabel(), rootMap.get(v.getLabel()) + 1));
                    }
            );

            List list = rootMap.entrySet().stream().filter(r -> r.getValue() == 0).collect(Collectors.toList());
            System.out.println(rootMap.toString());
            System.out.println(list.toString());

            rootSubgraph = rootMap.entrySet().stream().filter(r -> r.getValue() == 0).collect(Collectors.toList()).get(0).getKey();
        }

        System.out.println("Reading EPGM on sub-root: " + rootSubgraph);
        // Loop through the sub-graphs

        //Variable in lambda should be final
        String final_rootSubgraph = rootSubgraph;

        List<Vertex> papers = vertexes.stream().filter(v->v.getLabel().equals(final_rootSubgraph)).collect(Collectors.toList());
//        System.out.println(papers.size());

        vertexes.stream().filter(vertex -> vertex.getLabel().equals(final_rootSubgraph)).collect(Collectors.toList())
                .forEach(vertex -> {
                    Set<Attribute> attrSet = new HashSet<>();
                    attrSet.add(new Attribute("id", vertex.getId().toString()));
                    attrSet.add(new Attribute("label", vertex.getLabel()));
                    attrSet.add(new Attribute("version", vertex.getVersion().toString()));
                    vertex.getProperties().forEach( (k,v) -> attrSet.add(new Attribute(k, v.toString())) );

                    List<Pair<Vertex, Edge>> linkedVertex = new ArrayList<>();
                    edges.stream().filter(e -> e.getDst().equals(vertex.getId())).collect(Collectors.toList())
                            .forEach(e -> {
                                vertexes.stream().filter(vertex1 -> vertex1.getId()
                                        .equals(e.getSrc())).collect(Collectors.toList())
                                        .forEach(vertex2 -> {
                                            linkedVertex.add(new Pair<>(vertex2, e));
                                        });
                            });

                    Map<String, Set<String>> mapAttres = new HashMap<>();
                    linkedVertex.forEach( (pve) -> {
                        String key = "Vertex_" + pve.getValue().getLabel() + "_" + pve.getKey().getLabel();
                        if (mapAttres.containsKey(key)) {
                            Map<String, String> properties = pve.getKey().getProperties().entrySet().stream()
                                    .collect(Collectors.toMap(
                                            e->e.getKey(), e->e.getValue().toString()
                                    ));
                            properties.put("id", pve.getKey().getId().toString());
                            properties.put("version", pve.getKey().getVersion().toString());
                            properties.putAll(properties);

                            Set<String> tmp = new HashSet<>(mapAttres.get(key));
                            tmp.add(gson.toJson(properties));
                            mapAttres.put(key, tmp);
                        } else {
                            Map<String, String> properties = pve.getKey().getProperties().entrySet().stream()
                                    .collect(Collectors.toMap(
                                            e->e.getKey(), e->e.getValue().toString()
                                    ));
                            properties.put("id", pve.getKey().getId().toString());
                            properties.put("version", pve.getKey().getVersion().toString());
                            properties.putAll(properties);

                            Set<String> tmp = new HashSet<>();
                            tmp.add(gson.toJson(properties));
                            mapAttres.put(key, tmp);
                        }
                    });

                    mapAttres.forEach( (key,value) -> {
                        Attribute attr = new Attribute(key);
                        attr.addValues(value);
                        attrSet.add(attr);
                    } );

                    records.add(new Record(1.0, attrSet));
                });

        System.out.println("Read records: " + records.size());
    }

    private Vertex doVertexProperty(String key, String json) {

        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> mjson = gson.fromJson(json, type);
        ElementId id = ElementId.fromString(mjson.remove("id"));
        ElementId version = ElementId.fromString(mjson.remove("version"));
        Vertex vx = Vertex.create(id, new HashMap<>(), "", version);

        Map<String, PropertyValue> properties = new HashMap<>();
        mjson.forEach( (k,v) -> {
            properties.put(k, PropertyValue.create(v));
        });

        vx.setProperties(properties);
        return vx;
    }

    public void writeEPGMFromRecords(Set<Record> records, String fileOutput) {
        System.out.println("writeEPGMFromRecords: writing " + records.size() + " records to dir: " + fileOutput);
        StellarGraph graph = graphCollection.get(0);
        List<Vertex> vertexes = graph.getVertices().asList();
        List<Edge> edgesNew = new ArrayList<>();

        records.forEach(record -> {
            if (record == null) System.out.println("######################## empty record");
            Map<String, Attribute> attributes = record.getAttributes();

            Attribute id = attributes.get("id");

            if (id.getValuesCount() > 1) {
                Iterator iterId = id.iterator();
                List<String> idStack = new ArrayList<>();

                while (iterId.hasNext())
                    idStack.add((String)iterId.next());

                //FixMe: if the number of the matched IDs is too big, the double 'for loop' will take long time to generate edges.
                if (idStack.size() > 100) {
                    System.out.println("Deduplicated matcher threshold is too low. Please increase that threshold!");
                    return;
                }

                List<String> idStackHead = new ArrayList<>(idStack);

                for (int i = 0; i<idStack.size(); ++i) {
//                    int finalI = i;
//                    Vertex vsrc = vertexes.stream().filter(v -> v.getId().toString().equals(idStack.get(finalI))).collect(Collectors.toList()).get(0);
                    ElementId src = ElementId.fromString(idStack.get(i));
                    if (src != null) {
                        for (int j = i + 1; j < idStackHead.size(); ++j) {
//                            int finalJ = j;
//                            Vertex vdest = vertexes.stream().filter(v -> v.getId().toString().equals(idStackHead.get(finalJ))).collect(Collectors.toList()).get(0);
//                            if (dest != null) {
//                                edgesNew.add(Edge.create(src.getId(), dest.getId(), "duplicateOf"));
//                                edgesNew.add(Edge.create(dest.getId(), src.getId(), "duplicateOf"));
//                            }

                            ElementId dest = ElementId.fromString(idStackHead.get(j));
                            if (dest != null) {
                                edgesNew.add(Edge.create(src, dest, "duplicateOf"));
                                edgesNew.add(Edge.create(dest, src, "duplicateOf"));
                            }
                        }
                    }
                }
            }
        });

        if (edgesNew.size() > 0) {
            System.out.println("No. of new edges: " + edgesNew.size());
            StellarGraph graphNew = graph.unionEdges(stellarFactory.createMemory(edgesNew, Edge.class));
            // graphCollection.union(graphNew).write().json(fileOutput);

            // line changed by kevin's request
            graphNew.toCollection().write().json(fileOutput);
        } else
            System.out.println("No new edges generated, original graph has no change.");
    }
}