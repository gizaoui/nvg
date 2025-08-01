package com.logicbig.example;


public class ProjectParticipant {

    private final String name;
    private final String role;

    public ProjectParticipant(String name, String role) {
        this.name = name;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }


    @Override
    public String toString() {
        return "ProjectParticipant{" +
                "name='" + name + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}