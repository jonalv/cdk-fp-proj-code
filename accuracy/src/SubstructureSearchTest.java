import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jniinchi.INCHI_RET;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.FingerprinterTool;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.fragment.ExhaustiveFragmenter;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.io.iterator.IteratingMDLReader;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;


public class SubstructureSearchTest {

    public static class Structure {
        
        BitSet fingerprint;
        IAtomContainer atomContainer;
        
        public Structure(BitSet fingerprint, IAtomContainer atomContainer) {
            this.fingerprint = fingerprint;
            this.atomContainer = atomContainer;
        }
    }
    
    // inchi / fingerprint
    public Map<String, Structure> originals 
        = new HashMap<String, Structure>();
    public Map<String, Structure> fragments 
        = new HashMap<String, Structure>();
    private long truePositives;
    private long trueNegatives;
    private long falsePositives;
    private long falseNegatives;
    private long total;

    public SubstructureSearchTest( String fingerPrinterName, 
                                   List<String> files) throws Exception {
        IFingerprinter fper = getFingerprinter(fingerPrinterName);
        System.out.println("Running: " + fper);
        System.out.println("This program is a two step procedure, first " +
            "generating data then analazying data. It will give time " +
            "estimations for each such step separetly (also sperately for " +
            "each input file)");
        System.out.print("First counting the input structures...");
        long count = countStructures( files );
        System.out.println("done");
        long before = System.currentTimeMillis();
        long current = 1;

        for ( String file : files ) {
            IteratingMDLReader iterator = new IteratingMDLReader( 
                                              new FileInputStream(file), 
                                              DefaultChemObjectBuilder
                                                  .getInstance() );
            while (iterator.hasNext()) {
                IAtomContainer ac = (IAtomContainer) iterator.next();
                BitSet fp = fper.getFingerprint(ac);
                
                String inchiKey = generateInchiKey(ac);
                if ( inchiKey == null ) {
                    continue;
                }
                originals.put( inchiKey, new Structure( fp, ac ) );
                
                ExhaustiveFragmenter fragmenter = new ExhaustiveFragmenter();
                fragmenter.generateFragments( ac );
                IAtomContainer[] containers 
                    = fragmenter.getFragmentsAsContainers();
                System.out.println("Generated: " + containers.length);
                for ( IAtomContainer fragment 
                        : containers ) {
                    
                    // this if is not strictly needed but makes us skip 
                    // calculating fingerprints already done
                    inchiKey = generateInchiKey( fragment );
                    if ( inchiKey == null ) {
                        continue;
                    }
                    if ( !fragments.containsKey( inchiKey ) ) {
                        fragments.put( 
                            inchiKey, 
                            new Structure( fper.getFingerprint( fragment ), 
                                           fragment ) );
                    }
                }
                System.out.println(timeRemaining(before, count, current++) 
                                   + "for data generation.");
            }
        }
        
        before = System.currentTimeMillis();
        current = 1;
        for ( Structure fragment : fragments.values() ) {
            for ( Structure original : originals.values() ) {
                boolean FPMatch = FingerprinterTool.isSubset( 
                                      original.fingerprint, 
                                      fragment.fingerprint );            
                boolean TrueMatch = UniversalIsomorphismTester.isSubgraph(
                                        original.atomContainer,
                                        fragment.atomContainer );
                if (FPMatch && TrueMatch) {
                    truePositives++;
                }
                if (FPMatch && !TrueMatch) {
                    falsePositives++;
                }
                if (!FPMatch && TrueMatch) {
                    falseNegatives++;
                }
                if (!FPMatch && !TrueMatch) {
                    trueNegatives++;
                }
                total++;
            }
            System.out.println(timeRemaining(before, count, current++) 
                               + "for analysis.");
        }
        System.out.println("TRUE POSITIVES: " + truePositives );
        System.out.println("FALSE POSITIVES: " + falsePositives);
        System.out.println("TRUE NEGATIVES: " + trueNegatives);
        System.out.println("FALSE NEGATIVES:" + falseNegatives);
        System.out.println("ACCURACY: " 
            + new BigDecimal(truePositives + trueNegatives).divide( 
                      new BigDecimal( total ),
                      3,
                      BigDecimal.ROUND_HALF_UP ) );
    }

    private String timeRemaining( long before, long count, long current ) {
        double timeForOne 
            = (0.0 + System.currentTimeMillis() - before) / current;
        
        return "Expecting about " 
            + String.format("%.2f",(timeForOne*(count-current))/(1000*60*60))
            + " hours remaining ";
    }
    
    private String generateInchiKey( IAtomContainer ac ) throws CDKException {

        InChIGenerator gen = InChIGeneratorFactory.getInstance().getInChIGenerator( ac );
        if ( gen.getReturnStatus() != INCHI_RET.OKAY ) {
            System.err.println("inchi failed: " + gen.getMessage());
            return null;
        }
        return gen.getInchiKey();
    }

    public IFingerprinter getFingerprinter(String fingerPrinterName) 
                          throws Exception {
        return (IFingerprinter) Class.forName(fingerPrinterName)
                                     .newInstance();
    }

    // first fingerprinter name, then all sdf files
    public static void main(String[] args) throws Exception {
        long before = System.currentTimeMillis();
        List<String> arguments = Arrays.asList(args);
        SubstructureSearchTest t = 
            new SubstructureSearchTest( 
                    arguments.get(0), 
                    arguments.subList(1, arguments.size()) );
        System.out.println(
            "Complete run took: " 
            + String.format("%.2f", (System.currentTimeMillis()-before) 
                                    / 
                                    (1000*60*60.0) 
            + " hours"));
    }
    
    private long countStructures(List<String> files) 
                 throws FileNotFoundException {
        
        long count = 0;
        for ( String file : files ) {
            IteratingMDLReader iterator = new IteratingMDLReader( 
                                              new FileInputStream(file), 
                                              DefaultChemObjectBuilder
                                                  .getInstance() );
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
        }
        return count;
    }
}
