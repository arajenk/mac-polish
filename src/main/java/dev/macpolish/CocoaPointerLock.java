package dev.macpolish;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDLVideo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clears SDL's "window is moving or a focus click is pending" state when it has got stuck.
 *
 * <p>While that state is set, SDL's Cocoa_SetRelativeMouseMode returns early without locking the pointer
 * or hiding the cursor, even though SDL then reports relative mode as on. Clearing it through SDL's own
 * window listener makes SDL re-apply relative mode itself. Must be called on the main thread.
 *
 * <p>Every selector is checked first, so a future SDL that renames them turns this into a no-op.
 */
public final class CocoaPointerLock {
	private static final Logger LOGGER = LoggerFactory.getLogger("macpolish");
	private static final int MOUSE_BUTTONS_TO_CLEAR = 8;

	private static final MethodHandle MSG_PTR;
	private static final MethodHandle MSG_BOOL;
	private static final MethodHandle MSG_LONG;
	private static final MethodHandle MSG_VOID;
	private static final MethodHandle MSG_VOID_LONG;
	private static final MethodHandle MSG_RESPONDS;

	private static final MemorySegment SEL_RESPONDS;
	private static final MemorySegment SEL_DELEGATE;
	private static final MemorySegment SEL_IS_MOVING;
	private static final MemorySegment SEL_IS_PENDING;
	private static final MemorySegment SEL_FINISH_MOVING;
	private static final MemorySegment SEL_CLEAR_CLICK;
	private static final MemorySegment SEL_PRESSED_BUTTONS;
	private static final MemorySegment NS_EVENT;
	private static final boolean AVAILABLE;

	private static long cachedWindow;
	private static MemorySegment cachedListener = MemorySegment.NULL;

	static {
		MethodHandle ptr = null, bool = null, lng = null, vd = null, vdLong = null, responds = null;
		MemorySegment respondsSel = MemorySegment.NULL, delegate = MemorySegment.NULL, isMoving = MemorySegment.NULL, isPending = MemorySegment.NULL;
		MemorySegment finish = MemorySegment.NULL, clear = MemorySegment.NULL, pressed = MemorySegment.NULL, nsEvent = MemorySegment.NULL;
		boolean ok = false;
		try {
			Linker linker = Linker.nativeLinker();
			Arena arena = Arena.global();
			SymbolLookup objc = SymbolLookup.libraryLookup("/usr/lib/libobjc.A.dylib", arena);
			SymbolLookup.libraryLookup("/System/Library/Frameworks/AppKit.framework/AppKit", arena);
			MemorySegment send = objc.findOrThrow("objc_msgSend");
			ptr = linker.downcallHandle(send, FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
			bool = linker.downcallHandle(send, FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
			lng = linker.downcallHandle(send, FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
			vd = linker.downcallHandle(send, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
			vdLong = linker.downcallHandle(send, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
			responds = linker.downcallHandle(send, FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
			MethodHandle sel = linker.downcallHandle(objc.findOrThrow("sel_registerName"), FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
			MethodHandle cls = linker.downcallHandle(objc.findOrThrow("objc_getClass"), FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
			respondsSel = (MemorySegment) sel.invoke(arena.allocateFrom("respondsToSelector:"));
			delegate = (MemorySegment) sel.invoke(arena.allocateFrom("delegate"));
			isMoving = (MemorySegment) sel.invoke(arena.allocateFrom("isMoving"));
			isPending = (MemorySegment) sel.invoke(arena.allocateFrom("isMovingOrFocusClickPending"));
			finish = (MemorySegment) sel.invoke(arena.allocateFrom("windowDidFinishMoving"));
			clear = (MemorySegment) sel.invoke(arena.allocateFrom("clearFocusClickPending:"));
			pressed = (MemorySegment) sel.invoke(arena.allocateFrom("pressedMouseButtons"));
			nsEvent = (MemorySegment) cls.invoke(arena.allocateFrom("NSEvent"));
			ok = true;
		} catch (Throwable t) {
			LOGGER.warn("Pointer lock repair unavailable", t);
		}
		MSG_PTR = ptr;
		MSG_BOOL = bool;
		MSG_LONG = lng;
		MSG_VOID = vd;
		MSG_VOID_LONG = vdLong;
		MSG_RESPONDS = responds;
		SEL_RESPONDS = respondsSel;
		SEL_DELEGATE = delegate;
		SEL_IS_MOVING = isMoving;
		SEL_IS_PENDING = isPending;
		SEL_FINISH_MOVING = finish;
		SEL_CLEAR_CLICK = clear;
		SEL_PRESSED_BUTTONS = pressed;
		NS_EVENT = nsEvent;
		AVAILABLE = ok;
	}

	private CocoaPointerLock() {
	}

	private static MemorySegment listener(long sdlWindow) throws Throwable {
		if (sdlWindow != cachedWindow) {
			cachedWindow = sdlWindow;
			cachedListener = MemorySegment.NULL;
			long nsWindow = SDLProperties.SDL_GetPointerProperty(
					SDLVideo.SDL_GetWindowProperties(sdlWindow), SDLVideo.SDL_PROP_WINDOW_COCOA_WINDOW_POINTER, 0L);
			if (nsWindow != 0L) {
				MemorySegment delegate = (MemorySegment) MSG_PTR.invoke(MemorySegment.ofAddress(nsWindow), SEL_DELEGATE);
				if (delegate.address() != 0L
						&& (boolean) MSG_RESPONDS.invoke(delegate, SEL_RESPONDS, SEL_IS_MOVING)
						&& (boolean) MSG_RESPONDS.invoke(delegate, SEL_RESPONDS, SEL_IS_PENDING)
						&& (boolean) MSG_RESPONDS.invoke(delegate, SEL_RESPONDS, SEL_FINISH_MOVING)
						&& (boolean) MSG_RESPONDS.invoke(delegate, SEL_RESPONDS, SEL_CLEAR_CLICK)) {
					cachedListener = delegate;
				} else {
					LOGGER.warn("SDL window listener has an unexpected shape; pointer lock repair is off");
				}
			}
		}
		return cachedListener;
	}

	/**
	 * Clears the stuck state if there is one and no mouse button is physically held.
	 *
	 * @return true if SDL was stuck and has now been told to re-apply relative mode
	 */
	public static boolean clearIfStuck(long sdlWindow) {
		if (!AVAILABLE) {
			return false;
		}
		try {
			MemorySegment l = listener(sdlWindow);
			if (l.address() == 0L || !(boolean) MSG_BOOL.invoke(l, SEL_IS_PENDING)) {
				return false;
			}
			// A held button can mean a real click or drag is in progress, which SDL finishes by itself.
			if ((long) MSG_LONG.invoke(NS_EVENT, SEL_PRESSED_BUTTONS) != 0L) {
				return false;
			}
			if ((boolean) MSG_BOOL.invoke(l, SEL_IS_MOVING)) {
				MSG_VOID.invoke(l, SEL_FINISH_MOVING);
			}
			for (long button = 0; button < MOUSE_BUTTONS_TO_CLEAR; button++) {
				MSG_VOID_LONG.invoke(l, SEL_CLEAR_CLICK, button);
			}
			return true;
		} catch (Throwable t) {
			LOGGER.warn("Pointer lock repair failed", t);
			return false;
		}
	}
}
