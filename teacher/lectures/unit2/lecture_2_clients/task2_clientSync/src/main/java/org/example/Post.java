package org.example;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data                   // Generate getters, setters, toString(), equals(), hashCode()
@AllArgsConstructor     // constructor with all arguments
@NoArgsConstructor      // empty constructor(needed for Jackson deserialization)
public class Post {
    private int userId;
    private int id;
    private String title;
    private String body;
}
