package maven.miner.procedures;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import maven.miner.output.HelpResult;

public class HelpProc extends AbstractProcedureEnv {

	@Procedure("maven.miner.help")
    @Description("Provides descriptions of available procedures. To narrow the results, supply a search string. To also search in the description text, append : to the end of the search string.")
    public Stream<HelpResult> info(@Name(value = "procedure",defaultValue = "miner") String search) throws Exception {
		
		
		String [] items = search.split(":");	
        String name  = items[0].trim();
        if (items.length > 2) {
        	throw new RuntimeException("Unsupported search query. Please suplly a query in the form functionName[:description]");
        }
        String desc = " ";
        if (items.length == 2) desc = items[1].trim();
        
        String filter = " WHERE name starts with 'maven.' " +
			                " AND (n IS NULL  OR toLower(name) CONTAINS toLower(n) " +
			                " OR (toLower(description) CONTAINS toLower(d))) " +
			                " RETURN type, name, description, signature ";
        
        String query = String.format("WITH 'procedure' as type, '%s' as n, '%s' as d "
        		+ "CALL dbms.procedures() yield name, description, signature " + filter +
                " UNION ALL " +
                "WITH 'function' as type, '%s' as n, '%s' as d "
                + "CALL dbms.functions() yield name, description, signature " + filter
                , name
                , desc
                , name
                , desc);
        
        return graphDB.execute(query).stream().map(HelpResult::new);
        
    }
	
	public static <T,R> List<R> map(Stream<T> stream, Function<T,R> mapper) {
        return stream.map(mapper).collect(Collectors.toList());
    }

    public static <T,R> List<R> map(Collection<T> collection, Function<T,R> mapper) {
        return map(collection.stream(), mapper);
    }
}
