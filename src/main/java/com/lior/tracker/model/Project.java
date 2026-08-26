package com.lior.tracker.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Project {
    private String name;
    private Status status;
    private String lastUpdated;
    private String notes;
    public Project(String name, String status, String lastUpdated, String notes) {
        this.name = name;
        this.setStatus(status);
        this.lastUpdated = lastUpdated;
        this.notes = notes;
    }
    public enum Status {ACTIVE, BLOCKED, PAUSED, DONE}

    public Project() {}

    public void setName(String name) {this.name = name;}
    public String getName() {return this.name;}
    public void setStatus(String status) {
        if(status.equals("ACTIVE")) {this.status = Status.ACTIVE;}
        else if(status.equals("BLOCKED")) {this.status = Status.BLOCKED;}
        else if(status.equals("PAUSED")) {this.status = Status.PAUSED;}
        else if(status.equals("DONE")) {this.status = Status.DONE;}
    }
    public Status getStatus() {return this.status;}
    public void setLastUpdated(String lastUpdated) {this.lastUpdated = lastUpdated;}
    public String getLastUpdated() {return this.lastUpdated;}
    public void addNotes(String notes) {this.notes = (this.notes == null) ? notes : this.notes + "\n" + notes;}
    public String getNotes() {return this.notes;}
    public void setNotes(String notes) {this.notes = notes;}
    public static List<Project> getProjects(boolean print) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Project> projects = mapper.readValue(new File("projects.json"), new TypeReference<List<Project>>() {});
        if (!print) return projects;
        int i = 0;
        for (Project p : projects) {
            System.out.println("[" + i++ + "] " + p.getName() + " - " + p.getStatus());
        }
        return projects;
    }
}

