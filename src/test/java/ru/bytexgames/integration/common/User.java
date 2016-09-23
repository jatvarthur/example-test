package ru.bytexgames.integration.common;

public class User {
    public Integer id;
    public String firstName;
    public String lastName;

    public User() {
    }

    public User(Integer id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return String.format("User #%d (firstName='%s', lastName='%s')", id, firstName, lastName);
    }

}
