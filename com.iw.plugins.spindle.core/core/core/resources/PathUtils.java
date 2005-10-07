/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package core.resources;

import java.io.File;

import org.apache.hivemind.Resource;
import org.apache.tapestry.services.impl.ResourceLocalization;

import com.iw.plugins.spindle.core.resources.eclipse.ClasspathResource;

import core.util.Assert;

/**
 * The standard implementation of the <code>Path</code> interface. Paths are always maintained in
 * canonicalized form. That is, parent references (i.e., <code>../../</code>) and duplicate
 * separators are resolved. For example,
 * 
 * <pre>
 * new Path(&quot;/a/b&quot;).append(&quot;../foo/bar&quot;)
 * </pre>
 * 
 * will yield the path
 * 
 * <pre>
 *       /a/foo/bar
 * </pre>
 * 
 * <p>
 * This class is not intended to be subclassed by clients but may be instantiated.
 * </p>
 * 
 * @see Path
 */
public class PathUtils implements Cloneable
{

    /**
     * Path separator character constant "/" used in paths.
     */
    public static final char SEPARATOR = '/';

    /**
     * Device separator character constant ":" used in paths.
     */
    public static final char DEVICE_SEPARATOR = ':';

    /** The path segments */
    private String[] segments;

    /** The device id string. May be null if there is no device. */
    private String device = null;

    /** flags indicating separators (has leading, is UNC, has trailing) */
    private int separators;

    /** masks for separator values */
    private static final int HAS_LEADING = 1;

    private static final int IS_UNC = 2;

    private static final int HAS_TRAILING = 4;

    private static final int ALL_SEPARATORS = HAS_LEADING | IS_UNC | HAS_TRAILING;

    /** Mask for all bits that are involved in the hashcode */
    private static final int HASH_MASK = ~HAS_TRAILING;

    /** Constant value indicating no segments */
    private static final String[] NO_SEGMENTS = new String[0];

    /** Constant root path string (<code>"/"</code>). */
    private static final String ROOT_STRING = "/"; //$NON-NLS-1$

    /** Constant value containing the root path with no device. */
    public static final PathUtils ROOT = new PathUtils(ROOT_STRING);

    /** Constant empty string value. */
    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    private static final String[] EMPTY_STRING_ARRAY = new String[] {};

    /** Constant value containing the empty path with no device. */
    public static final PathUtils EMPTY = new PathUtils(EMPTY_STRING);

    public static String getFolderPath(Resource resource)
    {
        String path = resource.getPath();
        int lastSlashx = path.lastIndexOf('/');

        return path.substring(0, lastSlashx + 1);

    }

    // Private implementation note: the segments and separators
    // arrays are never modified, so that they can be shared between
    // path instances

    /*
     * (Intentionally not included in javadoc) Private constructor.
     */
    private PathUtils()
    {
        // not allowed
    }

    /*
     * (Intentionally not included in javadoc) Private constructor.
     */
    private PathUtils(String device, String[] segments, int _separators)
    {
        // no segment validations are done for performance reasons
        this.segments = segments;
        this.device = device;
        // hashcode is cached in all but the bottom three bits of the separators field
        this.separators = (computeHashCode() << 3) | (_separators & ALL_SEPARATORS);
    }

    /**
     * Constructs a new path from the given string path. The given string path must be valid. The
     * path is canonicalized and double slashes are removed except at the beginning. (to handle UNC
     * paths) All backslashes ('\') are replaced with forward slashes. ('/')
     * 
     * @param fullPath
     *            the string path
     * @see #isValidPath(String)
     */
    public PathUtils(String fullPath)
    {
        // no segment validations are done for performance reasons
        initialize(null, fullPath);
    }

    /**
     * Constructs a new path from the given device id and string path. The given string path must be
     * valid. The path is canonicalized and double slashes are removed except at the beginning (to
     * handle UNC paths). All backslashes ('\') are replaced with forward slashes. ('/')
     * 
     * @param device
     *            the device id
     * @param path
     *            the string path
     * @see #isValidPath(String)
     * @see #setDevice(String)
     */
    public PathUtils(String device, String path)
    {
        // no segment validations are done for performance reasons
        initialize(device, path);
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#addFileExtension
     */
    public PathUtils addFileExtension(String extension)
    {
        if (isRoot() || isEmpty() || hasTrailingSeparator())
            return this;
        int len = segments.length;
        String[] newSegments = new String[len];
        System.arraycopy(segments, 0, newSegments, 0, len - 1);
        newSegments[len - 1] = segments[len - 1] + "." + extension; //$NON-NLS-1$
        return new PathUtils(device, newSegments, separators);
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#addTrailingSeparator
     */
    public PathUtils addTrailingSeparator()
    {
        if (hasTrailingSeparator() || isRoot())
        {
            return this;
        }
        // XXX workaround, see 1GIGQ9V
        if (isEmpty())
        {
            return new PathUtils(device, segments, HAS_LEADING);
        }
        return new PathUtils(device, segments, separators | HAS_TRAILING);
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#append(java.lang.String)
     */
    public PathUtils append(String tail)
    {
        // optimize addition of a single segment
        if (tail.indexOf(SEPARATOR) == -1
                && tail.indexOf("\\") == -1 && tail.indexOf(DEVICE_SEPARATOR) == -1) { //$NON-NLS-1$
            int tailLength = tail.length();
            if (tailLength < 3)
            {
                // some special cases
                if (tailLength == 0 || ".".equals(tail)) { //$NON-NLS-1$
                    return this;
                }
                if ("..".equals(tail)) //$NON-NLS-1$
                    return removeLastSegments(1);
            }
            // just add the segment
            int myLen = segments.length;
            String[] newSegments = new String[myLen + 1];
            System.arraycopy(segments, 0, newSegments, 0, myLen);
            newSegments[myLen] = tail;
            return new PathUtils(device, newSegments, separators & ~HAS_TRAILING);
        }
        if (this.isEmpty())
            return new PathUtils(device, tail).makeRelative();
        if (this.isRoot())
            return new PathUtils(device, tail).makeAbsolute();

        // go with easy implementation
        return append(new PathUtils(tail));
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#append(Path)
     */
    public PathUtils append(PathUtils tail)
    {
        // optimize some easy cases
        if (tail == null || tail.segmentCount() == 0)
            return this;
        if (this.isEmpty())
            return tail.setDevice(device).makeRelative();
        if (this.isRoot())
            return tail.setDevice(device).makeAbsolute();

        // concatenate the two segment arrays
        int myLen = segments.length;
        int tailLen = tail.segmentCount();
        String[] newSegments = new String[myLen + tailLen];
        System.arraycopy(segments, 0, newSegments, 0, myLen);
        for (int i = 0; i < tailLen; i++)
        {
            newSegments[myLen + i] = tail.segment(i);
        }
        // use my leading separators and the tail's trailing separator
        PathUtils result = new PathUtils(device, newSegments, (separators & (HAS_LEADING | IS_UNC))
                | (tail.hasTrailingSeparator() ? HAS_TRAILING : 0));
        String tailFirstSegment = newSegments[myLen];
        if (tailFirstSegment.equals("..") || tailFirstSegment.equals(".")) { //$NON-NLS-1$ //$NON-NLS-2$
            result.canonicalize();
        }
        return result;
    }

    /**
     * Destructively converts this path to its canonical form.
     * <p>
     * In its canonical form, a path does not have any "." segments, and parent references ("..")
     * are collapsed where possible.
     * </p>
     * 
     * @return true if the path was modified, and false otherwise.
     */
    private boolean canonicalize()
    {
        // look for segments that need canonicalizing
        for (int i = 0, max = segments.length; i < max; i++)
        {
            String segment = segments[i];
            if (segment.charAt(0) == '.' && (segment.equals("..") || segment.equals("."))) { //$NON-NLS-1$ //$NON-NLS-2$
                // path needs to be canonicalized
                collapseParentReferences();
                // paths of length 0 have no trailing separator
                if (segments.length == 0)
                    separators &= (HAS_LEADING | IS_UNC);
                // recompute hash because canonicalize affects hash
                separators = (separators & ALL_SEPARATORS) | (computeHashCode() << 3);
                return true;
            }
        }
        return false;
    }

    /*
     * (Intentionally not included in javadoc) Clones this object.
     */
    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            return null;
        }
    }

    /**
     * Destructively removes all occurrences of ".." segments from this path.
     */
    private void collapseParentReferences()
    {
        int segmentCount = segments.length;
        String[] stack = new String[segmentCount];
        int stackPointer = 0;
        for (int i = 0; i < segmentCount; i++)
        {
            String segment = segments[i];
            if (segment.equals("..")) { //$NON-NLS-1$
                if (stackPointer == 0)
                {
                    // if the stack is empty we are going out of our scope
                    // so we need to accumulate segments. But only if the original
                    // path is relative. If it is absolute then we can't go any higher than
                    // root so simply toss the .. references.
                    if (!isAbsolute())
                        stack[stackPointer++] = segment; // stack push
                }
                else
                {
                    // if the top is '..' then we are accumulating segments so don't pop
                    if ("..".equals(stack[stackPointer - 1])) //$NON-NLS-1$
                        stack[stackPointer++] = ".."; //$NON-NLS-1$
                    else
                        stackPointer--;
                    // stack pop
                }
                // collapse current references
            }
            else if (!segment.equals(".") || (i == 0 && !isAbsolute())) //$NON-NLS-1$
                stack[stackPointer++] = segment; // stack push
        }
        // if the number of segments hasn't changed, then no modification needed
        if (stackPointer == segmentCount)
            return;
        // build the new segment array backwards by popping the stack
        String[] newSegments = new String[stackPointer];
        System.arraycopy(stack, 0, newSegments, 0, stackPointer);
        this.segments = newSegments;
    }

    /**
     * Removes duplicate slashes from the given path, with the exception of leading double slash
     * which represents a UNC path.
     */
    private String collapseSlashes(String path)
    {
        int length = path.length();
        // if the path is only 0, 1 or 2 chars long then it could not possibly have illegal
        // duplicate slashes.
        if (length < 3)
            return path;
        // check for an occurence of // in the path. Start at index 1 to ensure we skip leading UNC
        // //
        // If there are no // then there is nothing to collapse so just return.
        if (path.indexOf("//", 1) == -1) //$NON-NLS-1$
            return path;
        // We found an occurence of // in the path so do the slow collapse.
        char[] result = new char[path.length()];
        int count = 0;
        boolean hasPrevious = false;
        char[] characters = path.toCharArray();
        for (int index = 0; index < characters.length; index++)
        {
            char c = characters[index];
            if (c == SEPARATOR)
            {
                if (hasPrevious)
                {
                    // skip double slashes, except for beginning of UNC.
                    // note that a UNC path can't have a device.
                    if (device == null && index == 1)
                    {
                        result[count] = c;
                        count++;
                    }
                }
                else
                {
                    hasPrevious = true;
                    result[count] = c;
                    count++;
                }
            }
            else
            {
                hasPrevious = false;
                result[count] = c;
                count++;
            }
        }
        return new String(result, 0, count);
    }

    /*
     * (Intentionally not included in javadoc) Computes the hash code for this object.
     */
    private int computeHashCode()
    {
        int hash = device == null ? 17 : device.hashCode();
        int segmentCount = segments.length;
        for (int i = 0; i < segmentCount; i++)
        {
            // this function tends to given a fairly even distribution
            hash = hash * 37 + segments[i].hashCode();
        }
        return hash;
    }

    /*
     * (Intentionally not included in javadoc) Returns the size of the string that will be created
     * by toString or toOSString.
     */
    private int computeLength()
    {
        int length = 0;
        if (device != null)
            length += device.length();
        if ((separators & HAS_LEADING) != 0)
            length++;
        if ((separators & IS_UNC) != 0)
            length++;
        // add the segment lengths
        int max = segments.length;
        if (max > 0)
        {
            for (int i = 0; i < max; i++)
            {
                length += segments[i].length();
            }
            // add the separator lengths
            length += max - 1;
        }
        if ((separators & HAS_TRAILING) != 0)
            length++;
        return length;
    }

    /*
     * (Intentionally not included in javadoc) Returns the number of segments in the given path
     */
    private int computeSegmentCount(String path)
    {
        int len = path.length();
        if (len == 0 || (len == 1 && path.charAt(0) == SEPARATOR))
        {
            return 0;
        }
        int count = 1;
        int prev = -1;
        int i;
        while ((i = path.indexOf(SEPARATOR, prev + 1)) != -1)
        {
            if (i != prev + 1 && i != len)
            {
                ++count;
            }
            prev = i;
        }
        if (path.charAt(len - 1) == SEPARATOR)
        {
            --count;
        }
        return count;
    }

    /**
     * Computes the segment array for the given canonicalized path.
     */
    private String[] computeSegments(String path)
    {
        // performance sensitive --- avoid creating garbage
        int segmentCount = computeSegmentCount(path);
        if (segmentCount == 0)
            return NO_SEGMENTS;
        String[] newSegments = new String[segmentCount];
        int len = path.length();
        // check for initial slash
        int firstPosition = (path.charAt(0) == SEPARATOR) ? 1 : 0;
        // check for UNC
        if (firstPosition == 1 && len > 1 && (path.charAt(1) == SEPARATOR))
            firstPosition = 2;
        int lastPosition = (path.charAt(len - 1) != SEPARATOR) ? len - 1 : len - 2;
        // for non-empty paths, the number of segments is
        // the number of slashes plus 1, ignoring any leading
        // and trailing slashes
        int next = firstPosition;
        for (int i = 0; i < segmentCount; i++)
        {
            int start = next;
            int end = path.indexOf(SEPARATOR, next);
            if (end == -1)
            {
                newSegments[i] = path.substring(start, lastPosition + 1);
            }
            else
            {
                newSegments[i] = path.substring(start, end);
            }
            next = end + 1;
        }
        return newSegments;
    }

    /*
     * (Intentionally not included in javadoc) Compares objects for equality.
     */
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof PathUtils))
            return false;
        PathUtils target = (PathUtils) obj;
        // check leading separators and hashcode
        if ((separators & HASH_MASK) != (target.separators & HASH_MASK))
            return false;
        String[] targetSegments = target.segments;
        int i = segments.length;
        // check segment count
        if (i != targetSegments.length)
            return false;
        // check segments in reverse order - later segments more likely to differ
        while (--i >= 0)
            if (!segments[i].equals(targetSegments[i]))
                return false;
        // check device last (least likely to differ)
        return device == target.device || (device != null && device.equals(target.device));
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#getDevice
     */
    public String getDevice()
    {
        return device;
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#getFileExtension
     */
    public String getFileExtension()
    {
        if (hasTrailingSeparator())
        {
            return null;
        }
        String lastSegment = lastSegment();
        if (lastSegment == null)
        {
            return null;
        }
        int index = lastSegment.lastIndexOf("."); //$NON-NLS-1$
        if (index == -1)
        {
            return null;
        }
        return lastSegment.substring(index + 1);
    }

    /*
     * (Intentionally not included in javadoc) Computes the hash code for this object.
     */
    public int hashCode()
    {
        return separators & HASH_MASK;
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#hasTrailingSeparator
     */
    public boolean hasTrailingSeparator()
    {
        return (separators & HAS_TRAILING) != 0;
    }

    /*
     * Initialize the current path with the given string.
     */
    private void initialize(String device, String fullPath)
    {
        Assert.isNotNull(fullPath);
        this.device = device;

        // indexOf is much faster than replace
        String path = fullPath.indexOf('\\') == -1 ? fullPath : fullPath.replace('\\', SEPARATOR);

        int i = path.indexOf(DEVICE_SEPARATOR);
        if (i != -1)
        {
            // if the specified device is null then set it to
            // be whatever is defined in the path string
            if (device == null)
                this.device = path.substring(0, i + 1);
            path = path.substring(i + 1, path.length());
        }
        path = collapseSlashes(path);
        int len = path.length();

        // compute the separators array
        if (len < 2)
        {
            if (len == 1 && path.charAt(0) == SEPARATOR)
            {
                this.separators = HAS_LEADING;
            }
            else
            {
                this.separators = 0;
            }
        }
        else
        {
            boolean hasLeading = path.charAt(0) == SEPARATOR;
            boolean isUNC = hasLeading && path.charAt(1) == SEPARATOR;
            // UNC path of length two has no trailing separator
            boolean hasTrailing = !(isUNC && len == 2) && path.charAt(len - 1) == SEPARATOR;
            separators = hasLeading ? HAS_LEADING : 0;
            if (isUNC)
                separators |= IS_UNC;
            if (hasTrailing)
                separators |= HAS_TRAILING;
        }
        // compute segments and ensure canonical form
        segments = computeSegments(path);
        if (!canonicalize())
        {
            // compute hash now because canonicalize didn't need to do it
            separators = (separators & ALL_SEPARATORS) | (computeHashCode() << 3);
        }
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#isAbsolute
     */
    public boolean isAbsolute()
    {
        // it's absolute if it has a leading separator
        return (separators & HAS_LEADING) != 0;
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#isEmpty
     */
    public boolean isEmpty()
    {
        // true if no segments and no leading prefix
        return segments.length == 0 && ((separators & ALL_SEPARATORS) != HAS_LEADING);

    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#isPrefixOf
     */
    public boolean isPrefixOf(PathUtils anotherPath)
    {
        if (device == null)
        {
            if (anotherPath.getDevice() != null)
            {
                return false;
            }
        }
        else
        {
            if (!device.equalsIgnoreCase(anotherPath.getDevice()))
            {
                return false;
            }
        }
        if (isEmpty() || (isRoot() && anotherPath.isAbsolute()))
        {
            return true;
        }
        int len = segments.length;
        if (len > anotherPath.segmentCount())
        {
            return false;
        }
        for (int i = 0; i < len; i++)
        {
            if (!segments[i].equals(anotherPath.segment(i)))
                return false;
        }
        return true;
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#isRoot
     */
    public boolean isRoot()
    {
        // must have no segments, a leading separator, and not be a UNC path.
        return this == ROOT
                || (segments.length == 0 && ((separators & ALL_SEPARATORS) == HAS_LEADING));
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#isUNC
     */
    public boolean isUNC()
    {
        if (device != null)
            return false;
        return (separators & IS_UNC) != 0;
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#isValidPath
     */
    public boolean isValidPath(String path)
    {
        // We allow "//" at the beginning for UNC paths
        if (path.indexOf("//", 1) > 0) //$NON-NLS-1$
            return false;
        PathUtils test = new PathUtils(path);
        int segmentCount = test.segmentCount();
        for (int i = 0; i < segmentCount; i++)
        {
            if (!test.isValidSegment(test.segment(i)))
            {
                return false;
            }
        }
        return true;
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#isValidSegment
     */
    public boolean isValidSegment(String segment)
    {
        int size = segment.length();
        if (size == 0)
        {
            return false;
        }
        if (Character.isWhitespace(segment.charAt(0))
                || Character.isWhitespace(segment.charAt(size - 1)))
        {
            return false;
        }
        for (int i = 0; i < size; i++)
        {
            char c = segment.charAt(i);
            if (c == '/' || c == '\\' || c == ':')
            {
                return false;
            }
        }
        return true;
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#lastSegment
     */
    public String lastSegment()
    {
        int len = segments.length;
        return len == 0 ? null : segments[len - 1];
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#makeAbsolute
     */
    public PathUtils makeAbsolute()
    {
        if (isAbsolute())
        {
            return this;
        }
        PathUtils result = new PathUtils(device, segments, separators | HAS_LEADING);
        // may need canonicalizing if it has leading ".." or "." segments
        if (result.segmentCount() > 0)
        {
            String first = result.segment(0);
            if (first.equals("..") || first.equals(".")) { //$NON-NLS-1$ //$NON-NLS-2$
                result.canonicalize();
            }
        }
        return result;
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#makeRelative
     */
    public PathUtils makeRelative()
    {
        if (!isAbsolute())
        {
            return this;
        }
        return new PathUtils(device, segments, separators & HAS_TRAILING);
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#makeUNC
     */
    public PathUtils makeUNC(boolean toUNC)
    {
        // if we are already in the right form then just return
        if (!(toUNC ^ isUNC()))
            return this;

        int newSeparators = this.separators;
        if (toUNC)
        {
            newSeparators |= HAS_LEADING | IS_UNC;
        }
        else
        {
            // mask out the UNC bit
            newSeparators &= HAS_LEADING | HAS_TRAILING;
        }
        return new PathUtils(toUNC ? null : device, segments, newSeparators);
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#matchingFirstSegments
     */
    public int matchingFirstSegments(PathUtils anotherPath)
    {
        Assert.isNotNull(anotherPath);
        int anotherPathLen = anotherPath.segmentCount();
        int max = Math.min(segments.length, anotherPathLen);
        int count = 0;
        for (int i = 0; i < max; i++)
        {
            if (!segments[i].equals(anotherPath.segment(i)))
            {
                return count;
            }
            count++;
        }
        return count;
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#removeFileExtension
     */
    public PathUtils removeFileExtension()
    {
        String extension = getFileExtension();
        if (extension == null || extension.equals("")) { //$NON-NLS-1$
            return this;
        }
        String lastSegment = lastSegment();
        int index = lastSegment.lastIndexOf(extension) - 1;
        return removeLastSegments(1).append(lastSegment.substring(0, index));
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#removeFirstSegments
     */
    public PathUtils removeFirstSegments(int count)
    {
        if (count == 0)
            return this;
        if (count >= segments.length)
        {
            return new PathUtils(device, NO_SEGMENTS, 0);
        }
        Assert.isLegal(count > 0);
        int newSize = segments.length - count;
        String[] newSegments = new String[newSize];
        System.arraycopy(this.segments, count, newSegments, 0, newSize);

        // result is always a relative path
        return new PathUtils(device, newSegments, separators & HAS_TRAILING);
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#removeLastSegments
     */
    public PathUtils removeLastSegments(int count)
    {
        if (count == 0)
            return this;
        if (count >= segments.length)
        {
            // result will have no trailing separator
            return new PathUtils(device, NO_SEGMENTS, separators & (HAS_LEADING | IS_UNC));
        }
        Assert.isLegal(count > 0);
        int newSize = segments.length - count;
        String[] newSegments = new String[newSize];
        System.arraycopy(this.segments, 0, newSegments, 0, newSize);
        return new PathUtils(device, newSegments, separators);
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#removeTrailingSeparator
     */
    public PathUtils removeTrailingSeparator()
    {
        if (!hasTrailingSeparator())
        {
            return this;
        }
        return new PathUtils(device, segments, separators & (HAS_LEADING | IS_UNC));
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#segment
     */
    public String segment(int index)
    {
        if (index >= segments.length)
            return null;
        return segments[index];
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#segmentCount
     */
    public int segmentCount()
    {
        return segments.length;
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#segments
     */
    public String[] segments()
    {
        String[] segmentCopy = new String[segments.length];
        System.arraycopy(segments, 0, segmentCopy, 0, segments.length);
        return segmentCopy;
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#setDevice
     */
    public PathUtils setDevice(String value)
    {
        if (value != null)
        {
            Assert.isTrue(
                    value.indexOf(PathUtils.DEVICE_SEPARATOR) == (value.length() - 1),
                    "Last character should be the device separator"); //$NON-NLS-1$
        }
        // return the reciever if the device is the same
        if (value == device || (value != null && value.equals(device)))
            return this;

        return new PathUtils(value, segments, separators);
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#toFile
     */
    public File toFile()
    {
        return new File(toOSString());
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#toOSString
     */
    public String toOSString()
    {
        // Note that this method is identical to toString except
        // it uses the OS file separator instead of the path separator
        int resultSize = computeLength();
        if (resultSize <= 0)
            return EMPTY_STRING;
        char FILE_SEPARATOR = File.separatorChar;
        char[] result = new char[resultSize];
        int offset = 0;
        if (device != null)
        {
            int size = device.length();
            device.getChars(0, size, result, offset);
            offset += size;
        }
        if ((separators & HAS_LEADING) != 0)
            result[offset++] = FILE_SEPARATOR;
        if ((separators & IS_UNC) != 0)
            result[offset++] = FILE_SEPARATOR;
        int len = segments.length - 1;
        if (len >= 0)
        {
            // append all but the last segment, with separators
            for (int i = 0; i < len; i++)
            {
                int size = segments[i].length();
                segments[i].getChars(0, size, result, offset);
                offset += size;
                result[offset++] = FILE_SEPARATOR;
            }
            // append the last segment
            int size = segments[len].length();
            segments[len].getChars(0, size, result, offset);
            offset += size;
        }
        if ((separators & HAS_TRAILING) != 0)
            result[offset++] = FILE_SEPARATOR;
        return new String(result);
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#toString
     */
    public String toString()
    {
        int resultSize = computeLength();
        if (resultSize <= 0)
            return EMPTY_STRING;
        char[] result = new char[resultSize];
        int offset = 0;
        if (device != null)
        {
            int size = device.length();
            device.getChars(0, size, result, offset);
            offset += size;
        }
        if ((separators & HAS_LEADING) != 0)
            result[offset++] = SEPARATOR;
        if ((separators & IS_UNC) != 0)
            result[offset++] = SEPARATOR;
        int len = segments.length - 1;
        if (len >= 0)
        {
            // append all but the last segment, with separators
            for (int i = 0; i < len; i++)
            {
                int size = segments[i].length();
                segments[i].getChars(0, size, result, offset);
                offset += size;
                result[offset++] = SEPARATOR;
            }
            // append the last segment
            int size = segments[len].length();
            segments[len].getChars(0, size, result, offset);
            offset += size;
        }
        if ((separators & HAS_TRAILING) != 0)
            result[offset++] = SEPARATOR;
        return new String(result);
    }

    /*
     * (Intentionally not included in javadoc)
     * 
     * @see Path#uptoSegment
     */
    public PathUtils uptoSegment(int count)
    {
        if (count == 0)
            return new PathUtils(device, EMPTY_STRING_ARRAY, separators & (HAS_LEADING | IS_UNC));
        if (count >= segments.length)
            return this;
        Assert.isTrue(count > 0, "Invalid parameter to Path.uptoSegment"); //$NON-NLS-1$
        String[] newSegments = new String[count];
        System.arraycopy(segments, 0, newSegments, 0, count);
        return new PathUtils(device, newSegments, separators);
    }
}