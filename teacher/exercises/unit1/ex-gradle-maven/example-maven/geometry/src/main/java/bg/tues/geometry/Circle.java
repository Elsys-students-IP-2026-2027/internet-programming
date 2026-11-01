package bg.tues.geometry;

public record Circle(double radius) implements Shape {
    public double area() {
        return Math.PI * radius * radius;
    }
}
