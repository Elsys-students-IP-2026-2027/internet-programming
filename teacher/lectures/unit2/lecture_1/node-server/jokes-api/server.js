import express from "express";
import bodyParser from "body-parser";
import jokesRoutes from "./routes/jokes.js";

const app = express();
const port = 3000;

app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());


app.use("/jokes", jokesRoutes);

app.listen(port, () => {
    console.log(`Server started successfully on port ${port}`);
});
