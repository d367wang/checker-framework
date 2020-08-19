package org.checkerframework.checker.purity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import org.checkerframework.common.basetype.BaseTypeChecker;

public class PurityChecker extends BaseTypeChecker {
    @Override
    public void initChecker() {
        super.initChecker();
    }

    @Override
    public void typeProcessingOver() {
        // Reset ignore overflow.
        super.typeProcessingOver();

        // genCallGraph();

        PurityVisitor visitor = (PurityVisitor) getVisitor();
        Map<String, Map<String, String>> purityGroups = visitor.getPurityGroups();

        writeJson(purityGroups);
    }

    private void writeJson(Map<String, Map<String, String>> purityGroups) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Writer writer = new FileWriter("cf_output.json");
            gson.toJson(purityGroups, writer);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
