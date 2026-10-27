package bg.tues.geometry;

public record Rectangle(double width, double height) implements Shape {
    public double area() {
        return width * height;
    }
}
