package com.rein.solveur;

import com.rein.instance.Altruiste;
import com.rein.instance.Instance;
import com.rein.instance.Noeud;
import com.rein.instance.Paire;
import com.rein.interface_web.InterfaceWeb;
import com.rein.io.InstanceReader;
import com.rein.solution.Chaine;
import com.rein.solution.Solution;
import com.rein.transplantation.Cycle;
import com.rein.transplantation.Sequence;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class StrategieBasique2 implements Solveur{

    private Solveur solveur;
    @Override
    public String getNom() {
        return "StrategieBasique2"; }

    @Override
    public Solution solve(Instance instance) {
        Solution s = new Solution(instance);
        Noeud[] tabTest = instance.getTabNoeud();
        // Insertion des altruistes dans une chaine
        if(instance.getNbAltruistes()>0){
            int i;
            for(i=1;i<=instance.getNbAltruistes();i++){
                Altruiste a = (Altruiste) tabTest[0];
                Chaine ch = new Chaine(instance.getTailleMaxChaines(), a);
                tabTest= ArrayUtils.remove(tabTest,0);
                s.getListeSequences().add(ch);
            }
        }
        System.out.println(s.getListeSequences());
        for(int i=0;i<tabTest.length;i++) {
            boolean coutEffectue= false; // variable qui indique si la cout a ete effectue
            Paire pRecherche = (Paire) tabTest[0]; // premiere paire du tableau
            System.out.println("PAIRE SELECTIONNEE " + pRecherche);
            Cycle bestCycle = getMeilleurCycle(tabTest,pRecherche,instance.getTailleMaxCycles());

            System.out.println("TAILLE CYCLE "+bestCycle.getListeNoeuds().size());
            if (bestCycle.getListeNoeuds().size()>0) {
                for(Noeud nsupp : bestCycle.getListeNoeuds()){

                    if(isPresent(tabTest,nsupp)) {
                        System.out.println(" ou on le supp "+ recherchePlace(tabTest, nsupp));
                        tabTest=ArrayUtils.remove(tabTest, recherchePlace(tabTest, nsupp));
                    }
                }
                s.getListeSequences().add(bestCycle);
                coutEffectue = true;
            }

            //recherche insertion de la paire dans une chaine si elle n'a pas ete inseree
            if (!coutEffectue && s.getListeSequences().size()>0){
                System.out.println("Recherche Insertion Chaîne");
                boolean rechercheChaine = true;
                for (Sequence seq : s.getListeSequences()){
                    if (seq instanceof Chaine && rechercheChaine && seq.getListeNoeuds().size()>0){
                        Noeud nLast = seq.getListeNoeuds().get(seq.getListeNoeuds().size()-1);
                        if (nLast.isPossible(pRecherche)){
                            seq.getListeNoeuds().add(pRecherche);
                            rechercheChaine = false;
                            coutEffectue = true;
                            tabTest= ArrayUtils.remove(tabTest,0);
                        }
                    }
                }
            }
            // on supprime la paire du tableau de recherche (même si elle n'a pas été insérée)
            if (!coutEffectue){
                tabTest= ArrayUtils.remove(tabTest,0);
            }
            if (tabTest.length==0){
                System.out.println("tabTestVide");
                s.calculBenefice();
                return s;
            }
        }
        //calcul du bénéfice total
        s.calculBenefice();
        System.out.println(" Solution FINALE "+ s);
        return s;
    }
    // Fonction pour savoir si le noeud est présent dans le tableau des paires pas encore insérées dans la solution
    public boolean isPresent(Noeud[] tab, Noeud n){
        for(int i=0;i<tab.length;i++){
            if(tab[i].getId()==n.getId()){
                System.out.println("Présent => Noeud "+ n.getId());
                return true;
            }
        }
        return false;
    }
    //Fonction qui renvoie la place du noeud dans le tableau de recherche
    public int recherchePlace(Noeud[] tab, Noeud n){
        for(int i=0;i<tab.length;i++){
            if(tab[i].getId()==n.getId()){
                return i;
            }
        }
        return -1;
    }

    public Cycle getMeilleurCycle(Noeud[] tab,Noeud n,int tailleMax){
        Cycle cycleBest = new Cycle(tailleMax);

        for(int tTab = 0;tTab<tab.length;tTab++){
            Paire paire = (Paire) tab[tTab];
            if(n.isPossible(paire) && paire.isPossible(n)){
                Cycle cycleTest = new Cycle(tailleMax);
                cycleTest.ajouterNoeud(n,0);
                cycleTest.ajouterNoeud(paire,1);

                if(cycleBest.getBenefMedicalSequence()< cycleTest.getBenefMedicalSequence()){
                    cycleBest = cycleTest;
                }
            }

        }

        for(int tTab1 = 0;tTab1<tab.length;tTab1++){
            Paire paire = (Paire) tab[tTab1];

            for(int tTab2 = 1;tTab2<tab.length;tTab2++){

                Paire paire1 = (Paire) tab[tTab2];

                if(this.cycle3Possible(n,paire,paire1)){
                    Cycle cycleTest = new Cycle(tailleMax);
                    cycleTest.ajouterNoeud(n,0);
                    cycleTest.ajouterNoeud(paire,1);
                    cycleTest.ajouterNoeud(paire1,2);
                    if(cycleBest.getBenefMedicalSequence()< cycleTest.getBenefMedicalSequence()){
                        cycleBest = cycleTest;
                    }
                }
            }
        }

        return cycleBest;
    }

    public boolean cycle3Possible(Noeud n1, Noeud n2, Noeud n3){

        return (n1.isPossible(n2) && n2.isPossible(n3) && n3.isPossible(n1) &&
                n2.getId()!=n1.getId() &&n1.getId()!=n3.getId() && n2.getId()!=n3.getId());
    }

    public static void displayWebInterface(Solution s) throws IOException {
        int beneficeMedicalTotal = s.getBenefMedicalTotal();
        String begin = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "  <head>\n" +
                "    <script src=\"https://d3js.org/d3.v6.min.js\"></script>\n" +
                "    <script src=\"https://visjs.github.io/vis-network/standalone/umd/vis-network.min.js\"></script>\n" +
                "    <style type=\"text/css\">\n" +
                "      #mynetwork {\n" +
                "        width: 600px;\n" +
                "        height: 600px;\n" +
                "        border: 1px solid lightgray;\n" +
                "      }\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <div>" + s.getInstance().getNom() + "</div>\n" +
                "    <div id=\"results\">Bénéfice médical total : " + beneficeMedicalTotal + "</div>\n" +
                "    <div id=\"mynetwork\"></div>\n" +
                "    <script type=\"text/javascript\">";

        String nodes = "var nodes = new vis.DataSet([";
        for (Sequence sequence : s.getListeSequences()) {
            boolean isAltruiste = false;
            for (Noeud noeud : sequence.getListeNoeuds()) {
                int idNoeud = noeud.getId();
                String shape = "circle";
                String color = "orange";
                if (sequence instanceof Chaine && !isAltruiste) {
                    shape = "box";
                    color = "#97C2FC";
                    isAltruiste = true;
                }
                nodes += "{ id: " + idNoeud + ", label: \"" + idNoeud + "\", shape: \"" + shape + "\", color: \"" + color + "\" },";
            }
        }
        nodes = nodes.substring(0, nodes.length() - 1);
        nodes += "]);";

        String edges = "var edges = [";
        for (Sequence sequence : s.getListeSequences()) {
            for (int i = 0; i < sequence.getListeNoeuds().size(); i++) {
                // if it's not the last node of the sequence
                if (i + 1 < sequence.getListeNoeuds().size()) {
                    Noeud donneur = sequence.getListeNoeuds().get(i);
                    Noeud receveur = sequence.getListeNoeuds().get(i + 1);
                    int benefMedical = -1;
                    // CHAINE
                    if (sequence instanceof Chaine) {
                        System.out.println("chaine");
                        for (Map.Entry<Noeud, Integer> entry : sequence.getListeNoeuds().get(i).getListeEchanges().entrySet())
                            if (entry.getKey().getId() == receveur.getId())
                                benefMedical = entry.getValue();
                        edges += "{ from: " + donneur.getId() + ", to: " + receveur.getId() + ", label: \"" + benefMedical + "\", color: { color: \"black\" }, font: { align: \"top\" }, arrows: \"to\" },";
                    }
                    // CYCLE
                    if (sequence instanceof Cycle) {
                        System.out.println("cycle");
                        for (Map.Entry<Noeud, Integer> entry : donneur.getListeEchanges().entrySet())
                            if (entry.getKey().getId() == receveur.getId())
                                benefMedical = entry.getValue();
                        edges += "{ from: " + donneur.getId() + ", to: " + receveur.getId() + ", label: \"" + benefMedical + "\", color: { color: \"black\" }, font: { align: \"top\" }, arrows: \"from\" },";
                    }
                } else {
                    // CYCLE
                    if (sequence instanceof Cycle) {
                        System.out.println("cycle");
                        int benefMedical = -1;
                        Noeud donneur = sequence.getListeNoeuds().get(i);
                        Noeud receveur = sequence.getListeNoeuds().get(0);
                        for (Map.Entry<Noeud, Integer> entry : donneur.getListeEchanges().entrySet()) {
                            if (entry.getKey().getId() == receveur.getId()) {
                                benefMedical = entry.getValue();
                                edges += "{ from: " + donneur.getId() + ", to: " + receveur.getId() + ", label: \"" + benefMedical + "\", color: { color: \"black\" }, font: { align: \"top\" }, arrows: \"from\" },";
                            }
                        }
                    }
                }
            }
        }
        edges = edges.substring(0, edges.length() - 1);
        edges += "];";
        System.out.println(edges);
        String end = "var container = document.getElementById(\"mynetwork\");\n" +
                "      var data = {\n" +
                "        nodes: nodes,\n" +
                "        edges: edges,\n" +
                "      };\n" +
                "      var options = {};\n" +
                "      var network = new vis.Network(container, data, options);\n" +
                "    </script>\n" +
                "  </body>\n" +
                "</html>";

        String html = begin + nodes + edges + end;
        String pathname = "./results.html";
        File result = new File(pathname);
        result.createNewFile();
        FileWriter myWriter = new FileWriter(pathname);
        myWriter.write(html);
        myWriter.close();

    }

    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        // Création des options
        Options options = new Options();
        options.addOption(Option.builder("inst")
                .hasArg(true)
                .valueSeparator(' ')
                .desc("Nom du fichier d'instance")
                .build());
        options.addOption(Option.builder("dSol")
                .hasArg(true)
                .valueSeparator(' ')
                .desc("Répertoire des fichiers solutions")
                .build());
        try {
            // Lecture des arguments CLI
            CommandLine line = parser.parse(options, args);
            System.out.println(line.getOptionValue("inst"));
            System.out.println(line.getOptionValue("dSol"));
            InstanceReader reader;
            try {
                // Lecture du fichier d'instance
                reader = new InstanceReader("instancesInitiales/" + line.getOptionValue("inst"));
                Instance i = reader.readInstance();
                // Résolution de l'instance
                StrategieBasique2 sb = new StrategieBasique2();
                Solution s1 = sb.solve(i);
                System.out.println(s1);
                try {
                    // Création du fichier de solution
                    String nomFicSol = line.getOptionValue("dSol") + "/" + line.getOptionValue("inst").split("\\.")[0] + "_sol.txt";
                    File ficSol = new File(nomFicSol);
                    ficSol.getParentFile().mkdirs();
                    // Ecriture du fichier de solution
                    FileWriter myWriter = new FileWriter(ficSol);
                    myWriter.write(s1.exportSol());
                    myWriter.close();
                    System.out.println("Fichier de solution créé : " + nomFicSol);
                } catch (IOException e) {
                    System.err.println("ERROR fichier solution");
                    e.printStackTrace();
                }
                //System.out.println("s1 : " + s1.toString() + "\n\tcheck : " + s1.check());
                System.out.println("Checker : " + s1.check());
                new InterfaceWeb(s1).createHtmlFile();
            } catch(Exception e){
                System.out.println(e.getMessage());
                System.err.println("ERROR Strategie basique 2");
            }
        } catch (ParseException exp) {
            System.err.println("Unexpected exception:" + exp.getMessage());
        }
    }
}