package com.mpraski.jmonitor.adapters;

import static com.mpraski.jmonitor.util.TypeUtil.toDots;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.mpraski.jmonitor.EventMonitor;
import com.mpraski.jmonitor.EventOrder;
import com.mpraski.jmonitor.EventPatternMatcher;
import com.mpraski.jmonitor.EventType;

public class MethodAdapter extends MethodVisitor implements Opcodes {

	private final InstrumentationAdapter instrument;

	private final String thisName, thisDesc, thisRet;
	private final Map<EventType, List<EventPatternMatcher>> mapped;
	private final Map<EventPatternMatcher, Boolean> matchesFrom;

	private final List<EventData> eventsBefore, eventsAfter, eventsInstead;

	private EventType currentType;

	public MethodAdapter(String name, String desc, String source, String owner, InstrumentationAdapter instrument,
			List<EventPatternMatcher> matchers, Map<EventType, List<EventPatternMatcher>> mapped,
			Map<EventPatternMatcher, Boolean> matchesFrom, List<EventData> eventsBefore, List<EventData> eventsAfter,
			List<EventData> eventsInstead) {
		super(ASM5, instrument);

		this.instrument = instrument;

		this.thisName = toDots(owner) + '.' + name;
		this.thisDesc = desc;
		this.thisRet = toDots(Type.getMethodType(desc).getReturnType().getInternalName());
		this.mapped = mapped;
		this.matchesFrom = matchesFrom;

		matchesFrom.clear();

		/*
		 * Compute all the 'from' matches only once, they won't change throughout the
		 * method execution.
		 */
		for (EventPatternMatcher m : matchers)
			matchesFrom.put(m, m.matchesFrom(thisName));

		this.eventsBefore = eventsBefore;
		this.eventsAfter = eventsAfter;
		this.eventsInstead = eventsInstead;

		System.out.println("Visiting " + thisName + " | " + thisDesc);
	}

	@Override
	public void visitInsn(int opcode) {
		reset();

		switch (opcode) {
		case IRETURN:
		case LRETURN:
		case FRETURN:
		case DRETURN:
			tryMatch(EventType.RETURN, thisRet);
			break;
		case ARETURN:
			tryMatch(EventType.RETURN, thisRet);
			break;
		case RETURN:
			tryMatch(EventType.RETURN);
			break;
		case MONITORENTER:
			tryMatch(EventType.MONITOR_ENTER, toDots((String) instrument.getTop()));
			break;
		case MONITOREXIT:
			tryMatch(EventType.MONITOR_EXIT, toDots((String) instrument.getTop()));
			break;
		case ATHROW:
			tryMatch(EventType.THROW, toDots((String) instrument.getTop()));
			break;
		}

		instrumentBefore();
		instrumentInstead();
		mv.visitInsn(opcode);
		instrumentAfter();
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		reset();

		String fieldName = toDots(owner) + '.' + name;

		switch (opcode) {
		case GETFIELD:
			tryMatch(EventType.FIELD_READ, fieldName, name, desc, owner);
			break;
		case PUTFIELD:
			tryMatch(EventType.FIELD_WRITE, fieldName, name, desc, owner);
			break;
		case GETSTATIC:
			tryMatch(EventType.FIELD_READ_STATIC, fieldName, name, desc, owner);
			break;
		case PUTSTATIC:
			tryMatch(EventType.FIELD_WRITE_STATIC, fieldName, name, desc, owner);
			break;
		}

		instrumentBefore();
		instrumentInstead();
		if (instrument.shouldPreserveOriginal())
			mv.visitFieldInsn(opcode, owner, name, desc);
		instrumentAfter();
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		switch (opcode) {
		case ILOAD:
		case LLOAD:
		case FLOAD:
		case DLOAD:
		case ALOAD:
			break;
		case ISTORE:
		case LSTORE:
		case FSTORE:
		case DSTORE:
		case ASTORE:
			break;
		}

		mv.visitVarInsn(opcode, var);
	}

	@Override
	public void visitTypeInsn(int opcode, String desc) {
		reset();

		switch (opcode) {
		case NEW:
			tryMatch(EventType.INSTANCE, toDots(desc), EventOrder.BEFORE);
			break;
		case ANEWARRAY:
			tryMatch(EventType.INSTANCE_ARRAY, toDots(desc));
			break;
		}

		instrumentBefore();
		instrumentInstead();
		mv.visitTypeInsn(opcode, desc);
		instrumentAfter();
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
		reset();

		String methodOwner = toDots(owner);
		String methodName = methodOwner + '.' + name;
		Type type = Type.getMethodType(desc);

		switch (opcode) {
		case INVOKESPECIAL:
			if (name.equals("<init>")) {
				tryMatch(EventType.INSTANCE, methodOwner, EventOrder.AFTER);
				tryMatch(EventType.INSTANCE, methodName, type);
			} else {
				tryMatch(EventType.METHOD_CALL, methodName, type);
			}
			break;
		case INVOKESTATIC:
		case INVOKEVIRTUAL:
		case INVOKEINTERFACE:
			tryMatch(EventType.METHOD_CALL, methodName, type);
			break;
		}

		instrumentBefore();
		instrumentInstead();
		if (instrument.shouldPreserveOriginal())
			mv.visitMethodInsn(opcode, owner, name, desc, isInterface);
		instrumentAfter();
	}

	private void tryMatch(EventType type) {
		for (EventPatternMatcher m : mapped.get(type))
			if (matchesFrom.get(m))
				addMonitors(m);

		currentType = type;
	}

	private void tryMatch(EventType type, String of) {
		for (EventPatternMatcher m : mapped.get(type))
			if (matchesFrom.get(m) && m.matchesOf(of))
				addMonitors(m, of);

		currentType = type;
	}

	private void tryMatch(EventType type, String of, Type methodType) {
		for (EventPatternMatcher m : mapped.get(type))
			if (matchesFrom.get(m) && m.matchesOf(of))
				addMonitors(m, of, methodType);

		currentType = type;
	}

	private void tryMatch(EventType type, String of, EventOrder order) {
		for (EventPatternMatcher m : mapped.get(type))
			if (matchesFrom.get(m) && m.matchesOf(of))
				addMonitors(m, order, of);

		currentType = type;
	}

	private void tryMatch(EventType type, String of, String name, String desc, String owner) {
		for (EventPatternMatcher m : mapped.get(type))
			if (matchesFrom.get(m) && m.matchesOf(of))
				addMonitors(m, name, desc, owner);

		currentType = type;
	}

	private void reset() {
		currentType = null;
		eventsBefore.clear();
		eventsAfter.clear();
		eventsInstead.clear();
		instrument.reset();
	}

	private void addMonitors(EventPatternMatcher e) {
		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder());
			switch (m.getOrder()) {
			case BEFORE:
				eventsBefore.add(d);
				break;
			case AFTER:
				eventsAfter.add(d);
				break;
			case INSTEAD:
				eventsInstead.add(d);
				break;
			}
		}
	}

	private void addMonitors(EventPatternMatcher e, String of) {
		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder(), of);
			switch (m.getOrder()) {
			case BEFORE:
				eventsBefore.add(d);
				break;
			case AFTER:
				if (e.getType() == EventType.MONITOR_ENTER || e.getType() == EventType.MONITOR_EXIT)
					instrument.setShouldGenerateDup(true);

				eventsAfter.add(d);
				break;
			case INSTEAD:
				eventsInstead.add(d);
				break;
			}
		}
	}

	private void addMonitors(EventPatternMatcher e, String name, String desc, String owner) {
		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder(), name, desc, owner);
			switch (m.getOrder()) {
			case BEFORE:
				eventsBefore.add(d);
				break;
			case AFTER:
				eventsAfter.add(d);
				break;
			case INSTEAD:
				eventsInstead.add(d);
				break;
			}
		}

		if (e.getType() == EventType.FIELD_WRITE || e.getType() == EventType.FIELD_WRITE_STATIC)
			instrument.setShouldGenerateLocal(true);
	}

	private void addMonitors(EventPatternMatcher e, EventOrder order, String of) {
		for (EventMonitor m : e.getMonitors()) {
			if (m.getOrder() != order)
				continue;

			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder(), of);
			switch (order) {
			case BEFORE:
				eventsBefore.add(d);
				break;
			case AFTER:
				eventsAfter.add(d);
				break;
			case INSTEAD:
				eventsInstead.add(d);
				break;
			}
		}
	}

	private void addMonitors(EventPatternMatcher e, String of, Type methodType) {
		int numArgs = methodType.getArgumentTypes().length;
		String desc = methodType.getDescriptor();
		Type ret = methodType.getReturnType();

		for (EventMonitor m : e.getMonitors()) {
			EventData d = new EventData(e.getType(), e.getTag(), m.getFieldName(), m.getOrder(), of, numArgs, desc,
					ret);
			switch (m.getOrder()) {
			case BEFORE:
				eventsBefore.add(d);
				break;
			case AFTER:
				if (e.getType() == EventType.METHOD_CALL) {
					instrument.setShouldGenerateArgsArray(true);
					instrument.setNumArgs(numArgs);
				}

				eventsAfter.add(d);
				break;
			case INSTEAD:
				eventsInstead.add(d);
				break;
			}
		}
	}

	private void instrumentBefore() {
		if (currentType == null)
			return;

		if (instrument.shouldGenerateLocal()) {
			instrument.generateLocal();
		} else if (instrument.shouldGenerateArgsArray()) {
			instrument.generatedArgsArray();
		} else if (instrument.shouldGenerateDup()) {
			mv.visitInsn(DUP);
		}

		switch (currentType) {
		case FIELD_READ:
			eventsBefore.forEach(instrument::visitFieldRead);
			break;
		case FIELD_WRITE:
			eventsBefore.forEach(instrument::visitFieldWrite);
			break;
		case FIELD_READ_STATIC:
			eventsBefore.forEach(instrument::visitStaticFieldRead);
			break;
		case FIELD_WRITE_STATIC:
			eventsBefore.forEach(instrument::visitStaticFieldWrite);
			break;
		case METHOD_CALL:
			eventsBefore.forEach(instrument::visitMethodCallBefore);
			break;
		case RETURN:
		case THROW:
			eventsBefore.forEach(instrument::visitTopWithAutobox);
			break;
		case INSTANCE:
		case INSTANCE_ARRAY:
			eventsBefore.forEach(instrument::visitNewInstance);
			break;
		case MONITOR_ENTER:
		case MONITOR_EXIT:
			eventsBefore.forEach(instrument::visitTopWithAutobox);
			break;
		}
	}

	private void instrumentInstead() {
		if (currentType == null)
			return;

		switch (currentType) {
		case RETURN:
			eventsInstead.forEach(instrument::visitReturnInstead);
			break;
		case THROW:
			eventsInstead.forEach(instrument::visitThrowInstead);
			break;
		case MONITOR_ENTER:
		case MONITOR_EXIT:
			eventsInstead.forEach(instrument::visitTopWithSwap);
			break;
		case FIELD_READ:
			eventsInstead.forEach(instrument::visitReadInstead);
			break;
		case FIELD_WRITE:
			eventsInstead.forEach(instrument::visitWriteInstead);
			break;
		case METHOD_CALL:
			eventsInstead.forEach(instrument::visitMethodCallInstead);
			break;
		case INSTANCE:
			eventsInstead.forEach(instrument::visitInstanceInstead);
			break;
		}
	}

	private void instrumentAfter() {
		if (currentType == null)
			return;

		switch (currentType) {
		case FIELD_READ:
		case FIELD_READ_STATIC:
			eventsAfter.forEach(instrument::visitTopWithAutobox);
			break;
		case FIELD_WRITE:
			eventsAfter.forEach(instrument::visitFieldWrite);
			break;
		case FIELD_WRITE_STATIC:
			eventsAfter.forEach(instrument::visitStaticFieldWrite);
			break;
		case METHOD_CALL:
			eventsAfter.forEach(instrument::visitMethodCallAfter);
			break;
		case INSTANCE:
		case INSTANCE_ARRAY:
			eventsAfter.forEach(instrument::visitNewInstanceAfter);
			break;
		case MONITOR_ENTER:
			eventsAfter.forEach(instrument::visitTopWithAutobox);
			break;
		case MONITOR_EXIT:
			eventsAfter.forEach(instrument::visitTopWithSwap);
			break;
		}
	}

}
