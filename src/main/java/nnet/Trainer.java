package nnet;

import org.apache.lucene.index.IndexReader;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.nnet.Perceptron;
import parse.QrelsParser;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Trainer {
    private NeuralNetwork nnet;
    private DataSet trainingSet;

    public Trainer(){
        nnet = new NeuralNetwork();
    }

    public void loadDataset(String indexPath, String qrelsPath) throws IOException {
        var qp = new QrelsParser(qrelsPath);
        var d2v = new Doc2Vec(indexPath, "dummy");
        var index = d2v.index;
        while(qp.hasNext()){
            String docID = qp.currDoc();
            int topicID = qp.currTopic();
            double[] score = {qp.currScore()};
            //trainingSet.add();
        }
    }

    public void fit(){
        nnet.learn(trainingSet);
    }

    public double[] predict(double[] value){
        // set network input
        nnet.setInput(value);
        // calculate network
        nnet.calculate();
        // get network output
        double[] networkOutput = nnet.getOutput();
        return networkOutput;
    }

    public void save(String fileName){
        nnet.save(fileName);
    }

    public static void main(String[] args){
        // create new perceptron network
        NeuralNetwork neuralNetwork = new Perceptron(2, 1);
        // create training set
        DataSet trainingSet =
                new DataSet(2, 1);
        // add training data to training set (logical OR function)
        trainingSet.add(new DataSetRow(new double[]{0, 0},
                new double[]{0}));

        // learn the training set
        neuralNetwork.learn(trainingSet);
        // save the trained network into file
        neuralNetwork.save("or_perceptron.nnet");
    }
}
