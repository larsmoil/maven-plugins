package fix.test;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p>InterfaceWithNoJavadoc interface.</p>
 *
 * @author <a href="mailto:vsiveton@apache.org">vsiveton@apache.org</a>
 * @version $Id: $
 */
public interface InterfaceWithNoJavadoc
{
    /** Constant <code>MY_LIST_CONSTANT</code> */
    List<String> MY_LIST_CONSTANT = Collections.singletonList( "value" );

    /** Constant <code>MY_MAP_CONSTANT</code> */
    Map<String, String> MY_MAP_CONSTANT = Collections.singletonMap( "key", "value" );

    /**
     * <p>withGenericParameters</p>
     *
     * @param aList a {@link java.util.List} object.
     * @param aMap a {@link java.util.Map} object.
     */
    public void withGenericParameters( List<String> aList, Map<Map<String, List<String>>, List<String>> aMap );

    // ----------------------------------------------------------------------
    // New methods to be found by Clirr.
    // ----------------------------------------------------------------------

    /**
     * <p>newInterfaceMethod</p>
     *
     * @param aList a {@link java.util.List} object.
     * @param aMap a {@link java.util.Map} object.
     * @param <K> a K object.
     * @param <V> a V object.
     * @return a {@link java.util.Map} object.
     * @since 1.1
     */
    public <K, V> Map<K, V> newInterfaceMethod( List<String> aList, Map<Map<String, List<String>>, List<String>> aMap );
}
