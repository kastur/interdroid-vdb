package interdroid.vdb.content.avro;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class AvroRegistryService extends Service {
	public static final Logger logger =
			LoggerFactory.getLogger(AvroRegistryService.class);

	/**
	 * Handler of incoming messages from clients.
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Message reply = new Message();

			try {
				String schema = msg.getData().getString("schema");
				AvroProviderRegistry.registerSchema(AvroRegistryService.this.getBaseContext(), Schema.parse(schema));
				reply.what = 1;
			} catch (Exception e) {
				logger.warn("Error registering schema.", e);
				reply.what = 0;
			}

			try {
				msg.replyTo.send(reply);
			} catch (RemoteException e) {
				logger.warn("Error sending reply.", e);
			}
		}
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	/**
	 * When binding to the service, we return an interface to our messenger
	 * for sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

}
