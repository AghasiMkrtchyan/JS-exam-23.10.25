import express from "express";
import env from "dotenv";
import { v4 as uuidv4 } from 'uuid';
import { methodName} from './middleware/logger.js'
import { readFileSync, writeFileSync } from 'fs';


const id = uuidv4();
env.config();
const server = express();

server.use(methodName);
server.use(express.json());
server.use(express.static('public'));

server.get('/products', (req, res) => {
    const data = JSON.parse(readFileSync("./data/products.json", 'utf8'));
    res.json(data);
  });

   server.post('/products', (req, res) => {
    const data = JSON.parse(readFileSync("./data/products.json", 'utf8'));
    const newItem = { id: uuidv4(), age: req.body.age , product: req.body.product};
    if(isNaN(newItem.age) === false){
      if(newItem.age > 17){
    data.push(newItem);
    writeFileSync("./data/products.json", JSON.stringify(data, null, 2));
    res.status(201).json(newItem);}
    else{
      res.send("age must be big 18");
    }
  }else{
    res.send("age must be a number")
  }});

server.listen(process.env.PORT, () => {
    console.log(`server is listen on port ${process.env.PORT}`);
});