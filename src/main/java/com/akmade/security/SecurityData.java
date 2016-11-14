package com.akmade.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.akmade.messaging.Utility.invalidateMessage;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.akmade.messaging.api.Messenger.MessageAction;
import com.akmade.messaging.api.Messenger.MessageProtocol;
import com.akmade.messaging.api.ServiceController;
import com.akmade.messaging.api.queue.QueueListener;
import com.akmade.messaging.api.senders.demoperson.SecurityQueues;
import com.akmade.security.repositories.CommandRepo;
import com.akmade.security.repositories.QueryRepo;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.akmade.messaging.dto.MessagingDTO.BusMessage;
import com.akmade.messaging.security.dto.SecurityDTO;

import javax.jms.JMSException;


public class SecurityData extends ServiceController {
	Logger log = LoggerFactory.getLogger("Security Data Service");
	private QueueListener securityData;
	QueryRepo qryRepo = new QueryRepo();
	CommandRepo cmdRepo = new CommandRepo();
	
	private Function<Collection<? extends Message>, Collection<com.google.protobuf.Any>> makeAnys =
		objects ->  objects
						.stream()
						.map(o -> Any.pack(o))
						.collect(Collectors.toList());
		

		
	private <T extends Message> BusMessage makeNewBusMessage(BusMessage msg, T object, String errMsg) {
		try {
			return BusMessage.newBuilder()
					.addObjects(Any.pack(object))
					.setCommand("Reply: " + msg.getCommand())
					.setStatus(msg.getStatus())
					.build();
		} catch (Exception e) {
			log.error(errMsg, e);
			return invalidateMessage(msg, e, errMsg);
		}	
	}
		
	
	private BusMessage makeNewBusMessage(BusMessage msg, Collection<? extends Message> objects, String errMsg) {
		try {
			return BusMessage.newBuilder()
					.addAllObjects(makeAnys.apply(objects))
					.setCommand("Reply: " + msg.getCommand())
					.setStatus(msg.getStatus())
					.build();
		} catch (Exception e) {
			log.error(errMsg, e);
			return invalidateMessage(msg, e, errMsg);
		}
	}
	
	
	private Collection<? extends Message> getDTOFromMessage(BusMessage msg) {
		return msg
				.getObjectsList()
				.stream()
				.map(o -> {
					try {
						return o.unpack(SecurityDTO.Account.class);
					} catch (InvalidProtocolBufferException e) {
						e.printStackTrace();
						return null;
					}
				})
				.collect(Collectors.toList());
	}
	
	private final MessageAction getAccountByUsername = 
			msg -> makeNewBusMessage(msg, qryRepo.getAccountById.apply(0).get(), "Error getting the account");
	 
	private final MessageAction getAccounts = 
			m -> makeNewBusMessage(m, qryRepo.getAccounts.get(), "Error getting the accounts.");
			
	private final MessageAction getAddressTypes = 
		m -> makeNewBusMessage(m, qryRepo.getAddressTypes.get(), "Error getting the address types.");
	
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
