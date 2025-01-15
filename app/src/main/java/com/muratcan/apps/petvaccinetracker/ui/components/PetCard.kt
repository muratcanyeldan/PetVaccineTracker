package com.muratcan.apps.petvaccinetracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muratcan.apps.petvaccinetracker.R
import com.muratcan.apps.petvaccinetracker.model.Pet
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import java.text.DateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetCard(
    pet: Pet,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pet Image
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                if (pet.imageUri != null) {
                    GlideImage(
                        imageModel = { pet.imageUri },
                        modifier = Modifier.fillMaxSize(),
                        imageOptions = ImageOptions(
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center
                        ),
                        loading = {
                            Box(Modifier.fillMaxSize()) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        },
                        failure = {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                // Show placeholder icon
                            }
                        }
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        // Show placeholder icon
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Pet Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${pet.type} • ${pet.breed}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.born_on_date,
                        dateFormat.format(pet.birthDate)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 