package com.akmade.security.util;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akmade.exceptions.UnrecoverableException;
import com.akmade.security.util.Transaction.Txn;

public class RepositoryUtility {
	public static Logger logger = LoggerFactory.getLogger(RepositoryUtility.class);

	public static UnrecoverableException logAndThrowError(String msg) throws UnrecoverableException {
		logger.error(msg);
		return new UnrecoverableException(msg);
	}

	public static UnrecoverableException logAndThrowError(String msg, Exception e) throws UnrecoverableException {
		UnrecoverableException ex= new UnrecoverableException(msg + "\n" + e.getMessage());
		ex.addSuppressed(e);
		logger.error(msg, ex);
		return ex;
	}
	
	
	public static Txn doNothing =
			session -> {};
		
		
	public static Qry<Predicate<Object>> hasId =
		s -> 
			o -> {
				if (s.getIdentifier(o)!=null)
					return true;
				else
					return false;
			};
	
	public static Function<Object, Txn> delete =
		o ->
			s -> 
			{	
				if (hasId.execute(s).test(o))
					s.delete(o);
			};
				
	public static Function<Object, Txn> save =
		o ->
			s -> s.saveOrUpdate(o);
	
	public static Function<Function<Object, Txn>, Function<Object, Txn>> prepareTransaction =
			fn ->
				o -> o!=null?fn.apply(o):doNothing;
					
	public static <T> Txn prepareTransaction(Function<Object, Txn> txn, Collection<T> objects) {
		return prepareTransaction(txn, objects.stream());
	}
	
	public static <T> Txn prepareTransaction(Function<Object, Txn> txn, Stream<T> stream) {
		return stream
				.map(o -> prepareTransaction.apply(txn).apply(o))
				.reduce((t1, t2) -> t1.andThen(t2))
				.orElse(doNothing);
	}


}
