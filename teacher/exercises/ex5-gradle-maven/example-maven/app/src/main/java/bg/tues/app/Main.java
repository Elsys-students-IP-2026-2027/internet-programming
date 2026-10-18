package bg.tues.app;

import bg.tues.geometry.Circle;
import bg.tues.geometry.Rectangle;
import bg.tues.geometry.Shape;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Shape> shapes = List.of(new Circle(2), new Rectangle(3, 4));
        for (Shape s : shapes) {
            System.out.printf("%s -> площ %.2f%n", s, s.area());
        }
    }
}
