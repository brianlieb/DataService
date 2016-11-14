package com.akmade.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.akmade.messaging.Utility.invalidateMessage;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.akmade.messaging.api.Messenger.MessageAction;
import com.akmade.messaging.api.Messenger.MessageProtocol;
import com.akmade.messaging.api.ServiceController;
import com.akmade.messaging.api.queue.QueueListener;
import com.akmade.messaging.api.senders.demoperson.SecurityQueues;
import com.akmade.security.repositories.CommandRepo;
import com.akmade.security.repositories.QueryRepo;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.akmade.messaging.dto.MessagingDTO.BusMessage;

import javax.jms.JMSException;


public class SecurityData extends ServiceController {
	Logger log = LoggerFactory.getLogger("Security Data Service");
	private QueueListener securityData;
	QueryRepo qryRepo = new QueryRepo();
	CommandRepo cmdRepo = new CommandRepo();
	
	
	private Function<Message, com.google.protobuf.Any> makeAny =
		object ->  Any.pack(object);
		
		
	private Function<Collection<? extends Message>, Collection<com.google.protobuf.Any>> makeAnies =
		objects ->  objects
						.stream()
						.map(o -> Any.pack(o))
						.collect(Collectors.toList());
	 
	private final MessageAction getAccounts = 
			m -> {
				try {
					return BusMessage.newBuilder()
							.addAllObjects(makeAnies.apply(qryRepo.getAccounts.get()))
							.setCommand("Reply: " + m.getCommand())
							.setStatus(m.getStatus())
							.build();
				} catch (Exception e) {
					log.error("Error finding people", e);
					return invalidateMessage(m, e, "Security data encountered error getting address types.");
				}
			};
			
	private final MessageAction getAddressTypes = 
		m -> {
			try {
				return BusMessage.newBuilder()
						.addAllObjects(makeAnies.apply(qryRepo.getAddressTypes.get()))
						.setCommand("Reply: " + m.getCommand())
						.setStatus(m.getStatus())
						.build();
			} catch (Exception e) {
				log.error("Error finding people", e);
				return invalidateMessage(m, e, "Security data encountered error getting address types.");
			}
		};
	
	private MessageProtocol mProtocol = 
			m -> {
				BusMessage reply;
				switch (m.getCommand()) {
					case "get-address-types":
						reply = getAddressTypes.apply(m);
						break;
					default: 
						String s = "Security Service received unrecognized command: " + m.getCommand();
						log.error("Unrecognized command: " + m.getCommand());
						reply = invalidateMessage(m, s);
				}
				securityData.sendReply(reply);
			};
	
	protected SecurityData() throws JMSException {
		super();
		securityData = new QueueListener(SecurityQueues.SECURITY_DATA_QUEUE, mProtocol);
	}


}
