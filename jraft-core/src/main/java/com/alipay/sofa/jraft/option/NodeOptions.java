/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.jraft.option;

import com.alipay.remoting.util.StringUtils;
import com.alipay.sofa.jraft.JRaftServiceFactory;
import com.alipay.sofa.jraft.StateMachine;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.storage.SnapshotThrottle;
import com.alipay.sofa.jraft.util.Copiable;
import com.alipay.sofa.jraft.util.JRaftServiceLoader;
import com.alipay.sofa.jraft.util.Utils;

/**
 * Node options.
 *
 * @author boyan (boyan@alibaba-inc.com)
 *
 * 2018-Apr-04 2:59:12 PM
 */
public class NodeOptions extends RpcOptions implements Copiable<NodeOptions> {
    //根据spi加载JRaftServiceFactory的实现类，并获取优先级最高的实例
    public static final JRaftServiceFactory defaultServiceFactory  = JRaftServiceLoader.load(JRaftServiceFactory.class) //
                                                                       .first();

    // A follower would become a candidate if it doesn't receive any message
    // from the leader in |election_timeout_ms| milliseconds
    // Default: 1000 (1s)
    //如果一个follower在electionTimeoutMs内没有收到任何消息，那么这个Follower会变成候选者
    private int                             electionTimeoutMs      = 1000;                                         // follower to candidate timeout

    // Leader lease time's ratio of electionTimeoutMs,
    // To minimize the effects of clock drift, we should make that:
    // clockDrift + leaderLeaseTimeoutMs < electionTimeout
    // Default: 90, Max: 100
    private int                             leaderLeaseTimeRatio   = 90;

    // A snapshot saving would be triggered every |snapshot_interval_s| seconds
    // if this was reset as a positive number
    // If |snapshot_interval_s| <= 0, the time based snapshot would be disabled.
    //
    // Default: 3600 (1 hour)
    //每个小时会触发生成快照
    private int                             snapshotIntervalSecs   = 3600;

    // We will regard a adding peer as caught up if the margin between the
    // last_log_index of this peer and the last_log_index of leader is less than
    // |catchup_margin|
    //
    // Default: 1000
    private int                             catchupMargin          = 1000;

    // If node is starting from a empty environment (both LogStorage and
    // SnapshotStorage are empty), it would use |initial_conf| as the
    // configuration of the group, otherwise it would load configuration from
    // the existing environment.
    //
    // Default: A empty group
    private Configuration                   initialConf            = new Configuration();

    // The specific StateMachine implemented your business logic, which must be
    // a valid instance.
    //状态机
    private StateMachine                    fsm;

    // Describe a specific LogStorage in format ${type}://${parameters}
    private String                          logUri;

    // Describe a specific RaftMetaStorage in format ${type}://${parameters}
    private String                          raftMetaUri;

    // Describe a specific SnapshotStorage in format ${type}://${parameters}
    private String                          snapshotUri;

    // If enable, we will filter duplicate files before copy remote snapshot,
    // to avoid useless transmission. Two files in local and remote are duplicate,
    // only if they has the same filename and the same checksum (stored in file meta).
    // Default: false
    private boolean                         filterBeforeCopyRemote = false;

    // If non-null, we will pass this throughput_snapshot_throttle to SnapshotExecutor
    // Default: NULL
    //    scoped_refptr<SnapshotThrottle>* snapshot_throttle;

    // If true, RPCs through raft_cli will be denied.
    // Default: false
    private boolean                         disableCli             = false;

    /**
     * Timer manager thread pool size
     */
    private int                             timerPoolSize          = Utils.cpus() * 3 > 20 ? 20 : Utils.cpus() * 3;

    /**
     * CLI service request RPC executor pool size, use default executor if -1.
     */
    private int                             cliRpcThreadPoolSize   = Utils.cpus();
    /**
     * RAFT request RPC executor pool size, use default executor if -1.
     */
    private int                             raftRpcThreadPoolSize  = Utils.cpus() * 6;
    /**
     * Whether to enable metrics for node.
     */
    private boolean                         enableMetrics          = false;

    /**
     *  If non-null, we will pass this SnapshotThrottle to SnapshotExecutor
     * Default: NULL
     */
    private SnapshotThrottle                snapshotThrottle;

    /**
     * Custom service factory.
     */
    private JRaftServiceFactory             serviceFactory         = defaultServiceFactory;

    public JRaftServiceFactory getServiceFactory() {
        return this.serviceFactory;
    }

    public void setServiceFactory(final JRaftServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public SnapshotThrottle getSnapshotThrottle() {
        return this.snapshotThrottle;
    }

    public void setSnapshotThrottle(final SnapshotThrottle snapshotThrottle) {
        this.snapshotThrottle = snapshotThrottle;
    }

    public void setEnableMetrics(final boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }

    /**
     * Raft options
     */
    private RaftOptions raftOptions = new RaftOptions();

    public int getCliRpcThreadPoolSize() {
        return this.cliRpcThreadPoolSize;
    }

    public void setCliRpcThreadPoolSize(final int cliRpcThreadPoolSize) {
        this.cliRpcThreadPoolSize = cliRpcThreadPoolSize;
    }

    public boolean isEnableMetrics() {
        return this.enableMetrics;
    }

    public int getRaftRpcThreadPoolSize() {
        return this.raftRpcThreadPoolSize;
    }

    public void setRaftRpcThreadPoolSize(final int raftRpcThreadPoolSize) {
        this.raftRpcThreadPoolSize = raftRpcThreadPoolSize;
    }

    public int getTimerPoolSize() {
        return this.timerPoolSize;
    }

    public void setTimerPoolSize(final int timerPoolSize) {
        this.timerPoolSize = timerPoolSize;
    }

    public RaftOptions getRaftOptions() {
        return this.raftOptions;
    }

    public void setRaftOptions(final RaftOptions raftOptions) {
        this.raftOptions = raftOptions;
    }

    public void validate() {
        if (StringUtils.isBlank(this.logUri)) {
            throw new IllegalArgumentException("Blank logUri");
        }
        if (StringUtils.isBlank(this.raftMetaUri)) {
            throw new IllegalArgumentException("Blank raftMetaUri");
        }
        if (this.fsm == null) {
            throw new IllegalArgumentException("Null stateMachine");
        }
    }

    public int getElectionTimeoutMs() {
        return this.electionTimeoutMs;
    }

    public void setElectionTimeoutMs(final int electionTimeoutMs) {
        this.electionTimeoutMs = electionTimeoutMs;
    }

    public int getLeaderLeaseTimeRatio() {
        return this.leaderLeaseTimeRatio;
    }

    public void setLeaderLeaseTimeRatio(final int leaderLeaseTimeRatio) {
        if (leaderLeaseTimeRatio <= 0 || leaderLeaseTimeRatio > 100) {
            throw new IllegalArgumentException("leaderLeaseTimeRatio: " + leaderLeaseTimeRatio
                                               + " (expected: 0 < leaderLeaseTimeRatio <= 100)");
        }
        this.leaderLeaseTimeRatio = leaderLeaseTimeRatio;
    }

    public int getLeaderLeaseTimeoutMs() {
        return this.electionTimeoutMs * this.leaderLeaseTimeRatio / 100;
    }

    public int getSnapshotIntervalSecs() {
        return this.snapshotIntervalSecs;
    }

    public void setSnapshotIntervalSecs(final int snapshotIntervalSecs) {
        this.snapshotIntervalSecs = snapshotIntervalSecs;
    }

    public int getCatchupMargin() {
        return this.catchupMargin;
    }

    public void setCatchupMargin(final int catchupMargin) {
        this.catchupMargin = catchupMargin;
    }

    public Configuration getInitialConf() {
        return this.initialConf;
    }

    public void setInitialConf(final Configuration initialConf) {
        this.initialConf = initialConf;
    }

    public StateMachine getFsm() {
        return this.fsm;
    }

    public void setFsm(final StateMachine fsm) {
        this.fsm = fsm;
    }

    public String getLogUri() {
        return this.logUri;
    }

    public void setLogUri(final String logUri) {
        this.logUri = logUri;
    }

    public String getRaftMetaUri() {
        return this.raftMetaUri;
    }

    public void setRaftMetaUri(final String raftMetaUri) {
        this.raftMetaUri = raftMetaUri;
    }

    public String getSnapshotUri() {
        return this.snapshotUri;
    }

    public void setSnapshotUri(final String snapshotUri) {
        this.snapshotUri = snapshotUri;
    }

    public boolean isFilterBeforeCopyRemote() {
        return this.filterBeforeCopyRemote;
    }

    public void setFilterBeforeCopyRemote(final boolean filterBeforeCopyRemote) {
        this.filterBeforeCopyRemote = filterBeforeCopyRemote;
    }

    public boolean isDisableCli() {
        return this.disableCli;
    }

    public void setDisableCli(final boolean disableCli) {
        this.disableCli = disableCli;
    }

    @Override
    public NodeOptions copy() {
        final NodeOptions nodeOptions = new NodeOptions();
        nodeOptions.setElectionTimeoutMs(this.electionTimeoutMs);
        nodeOptions.setSnapshotIntervalSecs(this.snapshotIntervalSecs);
        nodeOptions.setCatchupMargin(this.catchupMargin);
        nodeOptions.setFilterBeforeCopyRemote(this.filterBeforeCopyRemote);
        nodeOptions.setDisableCli(this.disableCli);
        nodeOptions.setTimerPoolSize(this.timerPoolSize);
        nodeOptions.setCliRpcThreadPoolSize(this.cliRpcThreadPoolSize);
        nodeOptions.setRaftRpcThreadPoolSize(this.raftRpcThreadPoolSize);
        nodeOptions.setEnableMetrics(this.enableMetrics);
        nodeOptions.setRaftOptions(this.raftOptions == null ? new RaftOptions() : this.raftOptions.copy());
        return nodeOptions;
    }

    @Override
    public String toString() {
        return "NodeOptions [electionTimeoutMs=" + this.electionTimeoutMs + ", leaderLeaseTimeRatio="
               + this.leaderLeaseTimeRatio + ", snapshotIntervalSecs=" + this.snapshotIntervalSecs + ", catchupMargin="
               + this.catchupMargin + ", initialConf=" + this.initialConf + ", fsm=" + this.fsm + ", logUri="
               + this.logUri + ", raftMetaUri=" + this.raftMetaUri + ", snapshotUri=" + this.snapshotUri
               + ", filterBeforeCopyRemote=" + this.filterBeforeCopyRemote + ", disableCli=" + this.disableCli
               + ", timerPoolSize=" + this.timerPoolSize + ", cliRpcThreadPoolSize=" + this.cliRpcThreadPoolSize
               + ", raftRpcThreadPoolSize=" + this.raftRpcThreadPoolSize + ", enableMetrics=" + this.enableMetrics
               + ", snapshotThrottle=" + this.snapshotThrottle + ", serviceFactory=" + this.serviceFactory
               + ", raftOptions=" + this.raftOptions + "]";
    }
}
