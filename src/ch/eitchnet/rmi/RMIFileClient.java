package ch.eitchnet.rmi;

import java.rmi.RemoteException;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 * 
 */
public interface RMIFileClient {

	/**
	 * Remote method with which a client can push parts of files to the server. It is up to the client to send as many
	 * parts as needed, the server will write the parts to the associated file
	 * 
	 * @param filePart
	 *            the part of the file
	 * @throws RemoteException
	 *             if something goes wrong with the remote call
	 */
	public void uploadFilePart(RmiFilePart filePart) throws RemoteException;

	/**
	 * Remote method with which a client can delete files from the server. It only deletes single files if they exist
	 * 
	 * @param fileDeletion
	 *            the {@link RmiFileDeletion} defining the deletion request
	 * 
	 * @return true if the file was deleted, false if the file did not exist
	 * 
	 * @throws RemoteException
	 *             if something goes wrong with the remote call
	 */
	public boolean deleteFile(RmiFileDeletion fileDeletion) throws RemoteException;

	/**
	 * Remote method which a client can request part of a file. The server will fill the given {@link RmiFilePart} with
	 * a byte array of the file, with bytes from the file, respecting the desired offset. It is up to the client to call
	 * this method multiple times for the entire file. It is a decision of the concrete implementation how much data is
	 * returned in each part, the client may pass a request, but this is not definitive
	 * 
	 * @param filePart
	 *            the part of the file
	 * 
	 * @return the same file part, yet with the part of the file requested as a byte array
	 * 
	 * @throws RemoteException
	 *             if something goes wrong with the remote call
	 */
	public RmiFilePart requestFile(RmiFilePart filePart) throws RemoteException;
}
