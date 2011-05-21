import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.io.iterator.IteratingMDLReader;


public class SubstructureSearchTest {

	// inchi / fingerprint
	public Map<String, BitSet> fps = new HashMap<String, BitSet>();
	
	public SubstructureSearchTest(String fingerPrinterName, List<String> files) throws Exception {
		IFingerprinter fper = getFingerprinter(fingerPrinterName);
		System.out.println(fper);
		for ( String file : files ) {
			IteratingMDLReader iterator = new IteratingMDLReader( 
												new FileInputStream(file), 
												DefaultChemObjectBuilder.getInstance() );
			while (iterator.hasNext()) {
				IChemObject next = iterator.next();
				fper.getFingerprint(next);

			}
		}
	}
	
	public IFingerprinter getFingerprinter(String fingerPrinterName) throws Exception {
		return (IFingerprinter) Class.forName(fingerPrinterName).newInstance();
	}
	
	public static void main(String[] args) throws Exception {
		List<String> arguments = Arrays.asList(args);
		SubstructureSearchTest t = 
		new SubstructureSearchTest( arguments.get(0), 
				                    arguments.subList(1, arguments.size() - 1) );
		
	}
}
