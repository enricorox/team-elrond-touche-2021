package nnet;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.nnet.Perceptron;

public class Trainer {
    private NeuralNetwork nnet;

    public Trainer(){
        nnet = new NeuralNetwork();
    }

    public void loadDataset(){

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
