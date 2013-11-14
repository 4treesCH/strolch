package ch.eitchnet.xmlpers.api;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import ch.eitchnet.utils.helper.StringHelper;

public class TransactionResult {

	private String realm;
	private TransactionState state;
	private Exception failCause;

	private Date startTime;
	private long txDuration;
	private long closeDuration;

	private Map<String, ModificationResult> modificationByKey;

	/**
	 * @return the realm
	 */
	public String getRealm() {
		return this.realm;
	}

	/**
	 * @param realm
	 *            the realm to set
	 */
	public void setRealm(String realm) {
		this.realm = realm;
	}

	/**
	 * @return the state
	 */
	public TransactionState getState() {
		return this.state;
	}

	/**
	 * @param state
	 *            the state to set
	 */
	public void setState(TransactionState state) {
		this.state = state;
	}

	/**
	 * @return the failCause
	 */
	public Exception getFailCause() {
		return this.failCause;
	}

	/**
	 * @param failCause
	 *            the failCause to set
	 */
	public void setFailCause(Exception failCause) {
		this.failCause = failCause;
	}

	/**
	 * @return the startTime
	 */
	public Date getStartTime() {
		return this.startTime;
	}

	/**
	 * @param startTime
	 *            the startTime to set
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	/**
	 * @return the txDuration
	 */
	public long getTxDuration() {
		return this.txDuration;
	}

	/**
	 * @param txDuration
	 *            the txDuration to set
	 */
	public void setTxDuration(long txDuration) {
		this.txDuration = txDuration;
	}

	/**
	 * @return the closeDuration
	 */
	public long getCloseDuration() {
		return this.closeDuration;
	}

	/**
	 * @param closeDuration
	 *            the closeDuration to set
	 */
	public void setCloseDuration(long closeDuration) {
		this.closeDuration = closeDuration;
	}

	/**
	 * @return the modificationByKey
	 */
	public Map<String, ModificationResult> getModificationByKey() {
		return this.modificationByKey;
	}

	/**
	 * @param modificationByKey
	 *            the modificationByKey to set
	 */
	public void setModificationByKey(Map<String, ModificationResult> modificationByKey) {
		this.modificationByKey = modificationByKey;
	}

	/**
	 * @return
	 */
	public Set<String> getKeys() {
		return this.modificationByKey.keySet();
	}

	/**
	 * @param key
	 * @return
	 */
	public ModificationResult getModificationResult(String key) {
		return this.modificationByKey.get(key);
	}

	@SuppressWarnings("nls")
	public String getLogMessage() {

		int nrOfObjects = 0;
		for (ModificationResult result : this.modificationByKey.values()) {
			nrOfObjects += result.getCreated().size();
			nrOfObjects += result.getUpdated().size();
			nrOfObjects += result.getDeleted().size();
		}

		StringBuilder sb = new StringBuilder();
		switch (this.state) {
		case OPEN:
			sb.append("TX is still open after ");
			break;
		case COMMITTED:
			sb.append("TX was completed after ");
			break;
		case ROLLED_BACK:
			sb.append("TX was rolled back after ");
			break;
		case FAILED:
			sb.append("TX has failed after ");
			break;
		default:
			sb.append("TX is in unhandled state ");
			sb.append(this.state);
			sb.append(" after ");
		}

		sb.append(StringHelper.formatNanoDuration(this.txDuration));
		sb.append(" with close operation taking ");
		sb.append(StringHelper.formatNanoDuration(this.closeDuration));
		sb.append(". ");
		sb.append(nrOfObjects);
		sb.append(" objects in ");
		sb.append(this.modificationByKey.size());
		sb.append(" types were modified.");

		return sb.toString();
	}

	/**
	 * Clears all fields of this result, allowing it to be reused
	 */
	public void clear() {
		this.realm = null;
		this.state = null;
		this.failCause = null;
		this.startTime = null;
		this.txDuration = 0L;
		this.closeDuration = 0L;
	}
}
