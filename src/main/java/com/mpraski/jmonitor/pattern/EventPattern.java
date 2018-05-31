package com.mpraski.jmonitor.pattern;

import java.util.Objects;

import com.mpraski.jmonitor.event.EventType;

public final class EventPattern {
	private String tag;
	private String of, from, in;
	private EventMonitor beforeMonitor, afterMonitor, insteadMonitor;
	private EventPattern op1, op2;

	private final EventType type;

	public EventPattern(EventType type) {
		switch (type) {
		case AND:
		case OR:
		case NOT:
			throw new IllegalArgumentException("Cannot directly create a logical event");
		default:
			break;
		}

		this.type = type;
	}

	private EventPattern(EventType type, EventPattern op1) {
		this.type = type;

		this.op1 = Objects.requireNonNull(op1, "Op1 cannot be null").clone();
	}

	private EventPattern(EventType type, EventPattern op1, EventPattern op2) {
		this.type = type;

		this.op1 = Objects.requireNonNull(op1, "Op1 cannot be null").clone();
		this.op2 = Objects.requireNonNull(op2, "Op2 cannot be null").clone();
	}

	private EventPattern(EventPattern p) {
		if (p.beforeMonitor != null) {
			this.beforeMonitor = p.beforeMonitor.clone();
		}

		if (p.afterMonitor != null) {
			this.afterMonitor = p.afterMonitor.clone();
		}

		if (p.insteadMonitor != null) {
			this.insteadMonitor = p.insteadMonitor.clone();
		}

		this.tag = p.tag;
		this.of = p.of;
		this.from = p.from;
		this.in = p.in;
		this.type = p.type;

		if (p.op1 != null) {
			this.op1 = p.op1.clone();
		}

		if (p.op2 != null) {
			this.op2 = p.op2.clone();
		}
	}

	public EventPattern of(String of) {
		this.of = of;

		return clone();
	}

	public EventPattern from(String from) {
		this.from = from;

		return clone();
	}

	public EventPattern in(String in) {
		this.in = in;
		return clone();
	}

	public EventPattern setTag(String tag) {
		this.tag = tag;

		return clone();
	}

	// Monitor type and name
	public EventPattern doBefore(String monitor) {
		this.beforeMonitor = new EventMonitor(monitor, EventOrder.BEFORE);

		return clone();
	}

	public EventPattern doAfter(String monitor) {
		this.afterMonitor = new EventMonitor(monitor, EventOrder.AFTER);

		return clone();
	}

	public EventPattern doInstead(String monitor) {
		this.insteadMonitor = new EventMonitor(monitor, EventOrder.INSTEAD);

		return clone();
	}

	// Logical statements
	public EventPattern and(EventPattern other) {
		return new EventPattern(EventType.AND, this, other);
	}

	public EventPattern or(EventPattern other) {
		return new EventPattern(EventType.OR, this, other);
	}

	public EventPattern not() {
		return new EventPattern(EventType.NOT, this);
	}

	public EventPattern excluding(EventPattern other) {
		return new EventPattern(EventType.AND, this, other.not());
	}

	@Override
	public EventPattern clone() {
		return new EventPattern(this);
	}

	// Getters
	protected String getTag() {
		return tag;
	}

	protected String getOf() {
		return of;
	}

	protected String getFrom() {
		return from;
	}

	protected String getIn() {
		return in;
	}

	protected EventPattern getOp1() {
		return op1;
	}

	protected EventPattern getOp2() {
		return op2;
	}

	protected EventType getType() {
		return type;
	}

	protected EventMonitor getBeforeMonitor() {
		return beforeMonitor;
	}

	protected EventMonitor getAfterMonitor() {
		return afterMonitor;
	}

	protected EventMonitor getInsteadMonitor() {
		return insteadMonitor;
	}

	protected boolean hasMonitors() {
		return beforeMonitor != null || afterMonitor != null || insteadMonitor != null;
	}

	// Event pattern definitions
	public static EventPattern onMethodCall() {
		return new EventPattern(EventType.METHOD_CALL);
	}

	public static EventPattern onFieldRead() {
		return new EventPattern(EventType.FIELD_READ);
	}

	public static EventPattern onFieldWrite() {
		return new EventPattern(EventType.FIELD_WRITE);
	}

	public static EventPattern onStaticFieldRead() {
		return new EventPattern(EventType.FIELD_READ_STATIC);
	}

	public static EventPattern onStaticFieldWrite() {
		return new EventPattern(EventType.FIELD_WRITE_STATIC);
	}

	public static EventPattern onReturn() {
		return new EventPattern(EventType.RETURN);
	}

	public static EventPattern onThrow() {
		return new EventPattern(EventType.METHOD_CALL);
	}

	public static EventPattern onInstanceCreated() {
		return new EventPattern(EventType.INSTANCE);
	}

	public static EventPattern onArrayCreated() {
		return new EventPattern(EventType.INSTANCE_ARRAY);
	}

	public static EventPattern onMonitorEnter() {
		return new EventPattern(EventType.MONITOR_ENTER);
	}

	public static EventPattern onMonitorExit() {
		return new EventPattern(EventType.MONITOR_EXIT);
	}

	public static EventPattern onAnyEvent() {
		return new EventPattern(EventType.ANY);
	}
}
