package org.payjoin.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val asyncScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
