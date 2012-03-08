/*
 * Copyright (c) 2008-2012 Vrije Universiteit, The Netherlands All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the Vrije Universiteit nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package interdroid.vdb;

/**
 * This class holds constants for triggering actions in the vdb-ui package.
 * It has to live here due to the way the android tools structure Library
 * packages. We can't make vdb-ui a Library to import this class into
 * other UI packages that want to launch ui activities so it lives down
 * in this layer even though it really belongs in the vdb-ui system.
 *
 * @author nick
 *
 */
public final class Actions {
    /** Don't allow construction of this class. **/
    private Actions() { }

    /** Base for all actions. **/
    private static final String ACTION_BASE =
            "interdroid.vdb.action.";

    /** The Commit activity action. **/
    public static final String ACTION_COMMIT =
            ACTION_BASE + "COMMIT";

    /** Add a branch action. **/
    public static final String ACTION_ADD_BRANCH =
            ACTION_BASE + "ADD_BRANCH";

    /** Edit a remote hub action. **/
    public static final String ACTION_EDIT_REMOTE =
            ACTION_BASE + "EDIT_REMOTE";

    /** Add a peer action. **/
    public static final String ACTION_ADD_REMOTE =
            ACTION_BASE + "ADD_REMOTE";

    /** Manage local branches action. **/
    public static final String ACTION_MANAGE_LOCAL_BRANCHES =
            ACTION_BASE + "MANAGE_LOCAL_BRANCHES";

    /** Manage all remote repositories action. **/
    public static final String ACTION_MANAGE_REMOTES =
            ACTION_BASE + "MANAGE_REMOTES";

    /** Add a peer action. **/
    public static final String ACTION_ADD_PEER =
            ACTION_BASE + "ADD_PEER";

    /** Manage all peers action. **/
    public static final String ACTION_MANAGE_PEERS =
            ACTION_BASE + "MANAGE_PEERS";

    /** Manage the properties for a repository. **/
    public static final String ACTION_MANAGE_REPOSITORY_PROPERTIES =
            ACTION_BASE + "MANAGE_REPOSITORY_PROPERTIES";

    /** Manage a particular repository action. **/
    public static final String ACTION_MANAGE_REPOSITORY =
            ACTION_BASE + "MANAGE_REPOSITORY";

    /** Manage all repositories action. **/
    public static final String ACTION_MANAGE_REPOSITORIES =
            ACTION_BASE + "MANAGE_REPOSITORIES";

    /** Manage peer information action. **/
    public static final String ACTION_MANAGE_PEER_INFO =
            ACTION_BASE + "MANAGE_PEER_INFO";

    /** Manage local sharing action. **/
    public static final String ACTION_MANAGE_LOCAL_SHARING =
            ACTION_BASE + "MANAGE_LOCAL_SHARING";

    /** Manage remote sharing action. **/
    public static final String ACTION_MANAGE_REMOTE_SHARING =
            ACTION_BASE + "MANAGE_REMOTE_SHARING";

    /** The git service. **/
    public static final String GIT_SERVICE = "interdroid.vdb.GIT_SERVICE";

    /** Initialize a database. **/
    public static final String ACTION_INIT_DB = "interdroid.vdb.INIT_DB";

    /** Register an avro schema. **/
    public static final String REGISTER_SCHEMA =
            "interdroid.vdb.REGISTER_SCHEMA";

}
