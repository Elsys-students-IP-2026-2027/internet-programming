import express from "express";
import { jokes } from "../data/jokes-data.js";
import { masterKey } from "../config/constants.js";

const router = express.Router();

router.get("/random", (req, res) => {
    const randomIndex = Math.floor(Math.random() * jokes.length);
    res.json(jokes[randomIndex]);
});

router.get("/:id", (req, res) => {
    const id = parseInt(req.params.id);
    const foundJoke = jokes.find((joke) => joke.id === id);
    if (foundJoke) res.json(foundJoke);
    else res.status(404).json({ error: `Joke with id ${id} not found.` });
});

router.get("/", (req, res) => {
    const { type } = req.query;
    if (type) {
        const filtered = jokes.filter((joke) => joke.jokeType === type);
        res.json(filtered);
    } else {
        res.json(jokes);
    }
});

router.post("/", (req, res) => {
    const newJoke = {
        id: jokes.length + 1,
        jokeText: req.body.text,
        jokeType: req.body.type,
    };
    jokes.push(newJoke);
    res.status(201).json(newJoke);
});


router.put("/:id", (req, res) => {
    const id = parseInt(req.params.id);
    const index = jokes.findIndex((joke) => joke.id === id);
    if (index === -1) return res.status(404).json({ error: "Not found" });

    jokes[index] = { id, jokeText: req.body.text, jokeType: req.body.type };
    res.json(jokes[index]);
});


router.patch("/:id", (req, res) => {
    const id = parseInt(req.params.id);
    const index = jokes.findIndex((joke) => joke.id === id);
    if (index === -1) return res.status(404).json({ error: "Not found" });

    jokes[index] = {
        ...jokes[index],
        jokeText: req.body.text || jokes[index].jokeText,
        jokeType: req.body.type || jokes[index].jokeType,
    };
    res.json(jokes[index]);
});


router.delete("/:id", (req, res) => {
    const id = parseInt(req.params.id);
    const index = jokes.findIndex((joke) => joke.id === id);
    if (index > -1) {
        jokes.splice(index, 1);
        res.sendStatus(200);
    } else {
        res.status(404).json({ error: `Joke with id ${id} not found.` });
    }
});

router.delete("/", (req, res) => {
    const userKey = req.query.key;
    if (userKey === masterKey) {
        jokes.length = 0;
        res.sendStatus(200);
    } else {
        res.status(403).json({ error: "Unauthorized action." });
    }
});

export default router;
