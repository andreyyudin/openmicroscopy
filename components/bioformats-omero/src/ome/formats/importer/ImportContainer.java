package ome.formats.importer;

import java.io.File;

import ome.model.containers.Dataset;

public class ImportContainer {
    public File file;

    public Dataset dataset;

    public String imageName;

    public boolean archive;

    ImportContainer(File file, Dataset dataset, String imageName,
            boolean archive2) {
        this.file = file;
        this.dataset = dataset;
        this.imageName = imageName;
        this.archive = archive2;
    }
}
