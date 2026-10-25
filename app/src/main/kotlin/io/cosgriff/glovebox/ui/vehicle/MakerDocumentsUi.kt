package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ui.components.readBundled
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.MakerDocument
import io.cosgriff.glovebox.domain.MakerDocumentFile
import io.cosgriff.glovebox.domain.MakerDocumentKind
import io.cosgriff.glovebox.domain.MakerDocumentList
import io.cosgriff.glovebox.domain.MakerDocuments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Links to the makers' own documents that ship with the app: every file in `assets/maker-documents`,
 * read once.
 */
@Singleton
class MakerDocumentCatalog @Inject constructor(@ApplicationContext private val context: Context) {
    private val loaded: List<MakerDocumentList> by lazy {
        context.readBundled(MakerDocuments.ASSET_DIR) { (MakerDocuments.parse(it) as? MakerDocumentFile.Ok)?.list }
    }

    suspend fun lists(): List<MakerDocumentList> = withContext(Dispatchers.IO) { loaded }
}

/** What a maker's document is called on its row. */
val MakerDocument.titleRes: Int
    get() = when (kind) {
        MakerDocumentKind.OWNERS_MANUAL -> R.string.maker_doc_owners_manual
        MakerDocumentKind.MAINTENANCE_GUIDE -> R.string.maker_doc_maintenance_guide
        MakerDocumentKind.WARRANTY -> R.string.maker_doc_warranty
        MakerDocumentKind.FLUID_CHART -> R.string.maker_doc_fluid_chart
    }
